package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import org.bson.Document

import scala.io.Source
import scala.collection.JavaConverters._
import com.zhiwei.types.dbtypes.DBBaseType.{InsertElement, InsertElements, InsertValues, InsertValuesBatch, MongoDBManyDocuments}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.datasets.AbstractCollectionBuildReader
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro

object MoviesCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new MoviesCollectionReader(
        dataSetMacro,
        collectionSaverActorRef
      )
    )
}

class MoviesCollectionReader(
                              dataSetMacro: MovieLensDataSetMacro,
                              collectionSaverActorRef: ActorRef
                            ) extends AbstractCollectionBuildReader(
  dataSetMacro.moviesCollectionName,
) {
  val moviesCollectionName: String = dataSetMacro.moviesCollectionName
  val moviesFilePath: String = dataSetMacro.moviesFilePath
  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  val numProcessors: Int = Runtime.getRuntime.availableProcessors

  var numSendNoResponse: Int = 0

  val readIterator: Iterator[String] =
    Source
      .fromFile(moviesFilePath)
      .getLines()

  var readWorkDone: Boolean = !readIterator.hasNext

  val keys: List[String] = readIterator          // Iterator to the second line
    .next
    .split(",")
    .map(_.trim)
    .toList

  def getInsertValueList(): List[Option[InsertValues]] = {
    (0 to saveBatchSize)
      .toList
      .map(
        _ => {
          if (readIterator.hasNext)
            Some(
              readIterator
                .next
                .toLowerCase
                .split("," + "")
                .map(_.trim)
                .toList
            )
          else None
        }
      )
      .filter(_ .isDefined)
  }

  def processCols(
                   cols: InsertValues
                 ): InsertElement = {
    val movieId = cols(0)
    val title = cols.slice(1, cols.length - 1).foldLeft("")(_ + _ + ",")
    val genresString =
      if (cols.takeRight(1)(0) == "(no genres listed)") ""
      else cols.takeRight(1)(0)

    val year: String =
      if (title.contains(")")) {
        val lastIdx = title.lastIndexOf(")")
        val potentialYear = title.slice(lastIdx - 4, lastIdx)
        var ifYear = true
        potentialYear.foreach(
          char => if (!char.isDigit) ifYear = false
        )
        if (!ifYear) "" else potentialYear
      }
      else ""

    Map[String, String](
      "movieId" -> movieId,
      "title" -> title,
      "year" -> year,
      "genres" -> genresString
    )

  }

  def convertInsertElements2JavaDocs(
                                    insertElements: InsertElements
                                    ): MongoDBManyDocuments = {

    def convertInsertElementToDoc(
                                   insertElement: InsertElement
                                 ): Document = {
      val doc = new Document()
      insertElement.foreach(kv => doc.append(kv._1, kv._2))

      val movieId = doc.get("movieId", classOf[String]).toLong
      doc.remove("movieId")
      doc.append("movieId", movieId)

      val genresString = insertElement.getOrElse("genres", "")
      val genresJavaList =
        if (genresString == "") List[String]().asJava
        else
          genresString
            .toLowerCase
            .replace("|", ",")
            .split(",")
            .toList
            .asJava
      doc.remove("genres")
      doc.append("genres", genresJavaList)
      doc
    }

    insertElements
      .map(convertInsertElementToDoc)
      .asJava
  }

  def getSaveRequests(
                       n: Int
                     ): List[CollectionBuildSaveRequest] = {
    val insertValuesBatch: List[InsertValuesBatch] =
      (0 until n)
        .map(x => getInsertValueList().map(_.get))
        .toList
        .filter(_.nonEmpty)

    val insertElementsBatch: List[InsertElements] =
      insertValuesBatch.map(_.map(processCols))

    val nJavaDocs =
      insertElementsBatch
          .map(
            insertElements =>
              convertInsertElements2JavaDocs(insertElements)
          )

    nJavaDocs.map(CollectionBuildSaveRequest)
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
          context.parent ! CollectionBuildResult(moviesCollectionName, "Finished")
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