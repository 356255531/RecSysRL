package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import org.bson.Document

import scala.io.Source

import scala.collection.JavaConverters._

import com.zhiwei.datasets.AbstractCollectionBuildReader
import com.zhiwei.types.dbtypes.DBBaseType.{InsertElement, InsertElements, InsertValues, InsertValuesBatch, MongoDBManyDocuments}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.convertIterator2AnysWithBatchSize

object GenomeTagsCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new GenomeTagsCollectionReader(
        dataSetMacro: MovieLensDataSetMacro,
        collectionSaverActorRef: ActorRef
      )
    )
}

class GenomeTagsCollectionReader(
                                  dataSetMacro: MovieLensDataSetMacro,
                                  collectionSaverActorRef: ActorRef
                             )
  extends AbstractCollectionBuildReader(
    dataSetMacro.genomeTagsCollectionName,
  ) {

  val genomeTagsCollectionName: String = dataSetMacro.genomeTagsCollectionName
  val genomeTagsFilePath: String = dataSetMacro.genomeTagsFilePath
  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()

  var numSendNoResponse = 0

  val readIterator: Iterator[String] =
    Source
      .fromFile(genomeTagsFilePath)
      .getLines()

  var readWorkDone: Boolean = !readIterator.hasNext

  val keys: List[String] = readIterator          // Iterator to the second line
    .next
    .split(",")
    .map(_.trim)
    .toList

  def getInsertValues(): List[InsertValues] = {
    val insertValueStrings =
      convertIterator2AnysWithBatchSize(
        readIterator,
        saveBatchSize
      )

    insertValueStrings
      .map(
        _
          .toLowerCase
          .split(",")
          .map(_.trim)
          .toList
      )
  }

  def convertInsertValues2InsertElement(
                                           insertValues: InsertValues
                                       ): InsertElement = {
    keys
      .zip(insertValues)
      .toMap
  }

  def convertInsertElements2JavaDocs(
                                          insertElements: InsertElements
                                        ): MongoDBManyDocuments = {
    def convertInsertElement2Doc(
                                  insertElement: InsertElement
                                ): Document =  {
      val doc = new Document()
      doc.append("tagId", insertElement("tagId").toLong)
      doc.append("tag", insertElement("tag"))
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
      val saveRequests =
        getSaveRequests(numProcessors)
      saveRequests.foreach(collectionSaverActorRef ! _)
      numSendNoResponse += saveRequests.size
      if (!readIterator.hasNext) readWorkDone = true
    case CollectionBuildSaveResult("Finished") =>
      numSendNoResponse -= 1
      if (readWorkDone) {
        if (0 == numSendNoResponse) {
          context.parent ! CollectionBuildResult(genomeTagsCollectionName, "Finished")
        }
      }
      else {
        val saveRequests = getSaveRequests(1)
        saveRequests.foreach(collectionSaverActorRef ! _)
        numSendNoResponse += saveRequests.size
        if (!readIterator.hasNext)
          readWorkDone = true
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}
