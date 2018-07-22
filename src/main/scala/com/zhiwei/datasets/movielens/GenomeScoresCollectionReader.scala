package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}

import org.bson.Document

import scala.io.Source

import scala.collection.JavaConverters._

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection

import com.zhiwei.datasets.AbstractCollectionBuildReader
import com.zhiwei.types.dbtypes.DBBaseType.{InsertElement, InsertElements, InsertValues, InsertValuesBatch, MongoDBManyDocuments}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.convertIterator2AnysWithBatchSize

object GenomeScoresCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new GenomeScoresCollectionReader(
        dataSetMacro: MovieLensDataSetMacro,
        collectionSaverActorRef: ActorRef
      )
    )
}

class GenomeScoresCollectionReader(
                                    dataSetMacro: MovieLensDataSetMacro,
                                    collectionSaverActorRef: ActorRef
                             )
  extends AbstractCollectionBuildReader(
    dataSetMacro.genomeScoresCollectionName,
  ) {

  val dBName: String = dataSetMacro.dBName
  val genomeScoresCollectionName: String = dataSetMacro.genomeScoresCollectionName
  val genomeTagsCollectionName: String = dataSetMacro.genomeTagsCollectionName
  val genomeScoresFilePath: String = dataSetMacro.genomeScoresFilePath
  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  val client = new MongoClient()
  val genomeTagsCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(genomeTagsCollectionName)

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()

  var numSendNoResponse: Int = 0

  val readIterator: Iterator[String] =
    Source
      .fromFile(genomeScoresFilePath)
      .getLines()

  var readWorkDone: Boolean = !readIterator.hasNext

  val keys: List[String] = readIterator          // Iterator to the second line
    .next
    .split(",")
    .map(_.trim)
    .toList

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getInsertValues(): List[InsertValues] = {
    val insertValueStrings =
      convertIterator2AnysWithBatchSize(readIterator, saveBatchSize)

    insertValueStrings
      .map(
        _
          .toLowerCase()
          .split(",")
          .map(_.trim)
          .toList
      )
  }

  def convertInsertValues2InsertElement(
                                           insertValues: InsertValues
                                       ): InsertElement = {
    val insertElement = keys
      .zip(insertValues)
      .toMap

    insertElement + (
      "relevance" ->
        insertElement("relevance")
          .toDouble
          .formatted("%.3f")
          .toString
      )
  }

  def convertInsertElements2JavaDocs(
                                          insertElements: InsertElements
                                        ): MongoDBManyDocuments = {
    def convertInsertElement2Doc(
                                  insertElement: InsertElement
                                ): Document =  {
      val doc = new Document()

      val tagId = insertElement("tagId").toInt
      val tag =
        genomeTagsCollection
          .find(new Document("tagId", tagId))
          .limit(1)
          .iterator
          .next
          .get("tag")
      doc.append("tag", tag)

      doc.append("tagId", insertElement("tagId").toInt)
      doc.append("movieId", insertElement("movieId").toInt)
      doc.append("relevance", insertElement("relevance").toDouble)

      doc
    }

    insertElements
      .map(insertElement => convertInsertElement2Doc(insertElement))
      .asJava
  }

  def getSaveRequests(
                       n: Int
                     ): List[CollectionBuildSaveRequest] = {
    val insertValuesBatches: List[InsertValuesBatch] =
      (0 until n)
        .toList
        .map(x => getInsertValues())
        .filter(_.nonEmpty)

    val insertElementsBatches: List[InsertElements] =
      insertValuesBatches
        .map(
          _.map(
            convertInsertValues2InsertElement
          )
        )

    val javaDocs: List[MongoDBManyDocuments] =
      insertElementsBatches
        .map(convertInsertElements2JavaDocs)

    javaDocs.map(CollectionBuildSaveRequest)
  }

  def receive: PartialFunction[Any, Unit] = {
    case CollectionBuildRequest =>
      if (readWorkDone) throw new ExceptionInInitializerError("File empty!")
      val saveRequests = getSaveRequests(numProcessors)
      readWorkDone = !readIterator.hasNext
      saveRequests.foreach(collectionSaverActorRef ! _)
      numSendNoResponse += saveRequests.size
    case CollectionBuildSaveResult("Finished") =>
      numSendNoResponse -= 1
      if (readWorkDone) {
        if (0 == numSendNoResponse) {
          context.parent ! CollectionBuildResult(genomeScoresCollectionName, "Finished")
        }
      }
      else {
        val saveRequests = getSaveRequests(1)
        readWorkDone = !readIterator.hasNext
        saveRequests.foreach(collectionSaverActorRef ! _)
        numSendNoResponse += saveRequests.size
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}
