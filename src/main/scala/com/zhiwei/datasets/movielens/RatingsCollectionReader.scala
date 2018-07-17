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

object RatingsCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new RatingsCollectionReader(
        dataSetMacro: MovieLensDataSetMacro,
        collectionSaverActorRef: ActorRef
      )
    )
}

class RatingsCollectionReader(
                               dataSetMacro: MovieLensDataSetMacro,
                               collectionSaverActorRef: ActorRef
                             )
  extends AbstractCollectionBuildReader(
    dataSetMacro.ratingsCollectionName
  ) {
  val ratingsCollectionName: String = dataSetMacro.ratingsCollectionName
  val ratingsFilePath: String = dataSetMacro.ratingsFilePath
  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  val numProcessors: Int = Runtime.getRuntime.availableProcessors

  var numSendNoResponse: Int = 0

  val readIterator: Iterator[String] =
    Source
      .fromFile(ratingsFilePath)
      .getLines()

  var readWorkDone: Boolean = !readIterator.hasNext

  val keys: List[String] = readIterator          // Iterator to the second line
    .next
    .split(",")
    .map(_.trim)
    .toList

  def getInsertValues(): List[InsertValues] = {
    (0 to saveBatchSize)
      .toList
      .map(
        x => {
          if (readIterator.hasNext)
            Some(
              readIterator
                .next
                .split(",")
                .map(_.trim)
                .toList
            )
          else None
        }
      )
      .filter(_.isDefined)
      .map(_.get)
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

    insertElements
      .map(
        insertElement => {
          val doc = new Document()
          doc.append("userId", insertElement("userId").toLong)
          doc.append("movieId", insertElement("movieId").toLong)
          doc.append("rating", insertElement("rating").toDouble)
          doc.append("timestamp", insertElement("timestamp").toLong)
          doc
        }
      )
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
          context.parent ! CollectionBuildResult(ratingsCollectionName, "Finished")
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
