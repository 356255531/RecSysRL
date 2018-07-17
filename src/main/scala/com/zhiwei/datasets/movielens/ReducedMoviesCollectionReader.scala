package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import scala.collection.JavaConverters._
import org.bson.Document
import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoCursor}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.utils.{throwNotImplementedError, convertMongoCursor2Anys}
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.datasets.AbstractCollectionBuildReader
import com.zhiwei.types.dbtypes.DBBaseType.Documents

object ReducedMoviesCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new ReducedMoviesCollectionReader(
        dataSetMacro,
        collectionSaverActorRef
      )
    )
}

class ReducedMoviesCollectionReader(
                                      dataSetMacro: MovieLensDataSetMacro,
                                      collectionSaverActorRef: ActorRef
                           ) extends AbstractCollectionBuildReader(
                                          dataSetMacro.reducedMoviesCollectionName
) {
  val dBName: String = dataSetMacro.dBName
  val reducedMoviesCollectionName: String = dataSetMacro.reducedMoviesCollectionName
  val moviesCollectionName: String = dataSetMacro.moviesCollectionName
  val reducedRatingsCollectionName: String = dataSetMacro.reducedRatingsCollectionName
  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  val client: MongoClient = new MongoClient()
  val moviesCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(moviesCollectionName)
  val reducedRatingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(reducedRatingsCollectionName)

  val reducedRatingMovieIdsIterator: MongoCursor[java.lang.Long] =
    reducedRatingsCollection.distinct("movieId", classOf[java.lang.Long]).iterator
  val reducedRatingMovieIds: List[Long] =
    convertMongoCursor2Anys(reducedRatingMovieIdsIterator).map(_.toLong)

  val readIterator: MongoCursor[Document] = moviesCollection.find.sort(new Document("movieId", 1)).iterator

  var queryWorkDone: Boolean = !readIterator.hasNext

  var counter = 0

  val numProcessors: Int = Runtime.getRuntime.availableProcessors

  var numSendNoResponse: Int = 0

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getSaveRequestOption(): Option[CollectionBuildSaveRequest] =
  {
    val movieDocs: Documents =
      (0 until saveBatchSize)
        .toList
        .map(
          x =>
            if (readIterator.hasNext)
              Some(readIterator.next)
            else None
        )
        .filter(_ .isDefined)
        .map(_.get)

    val reducedMovieDocs: Documents =
      movieDocs.filter(
        movieDoc =>
          reducedRatingMovieIds.contains(
            movieDoc.get("movieId", classOf[java.lang.Long]).toLong
          )
      )

    reducedMovieDocs.foreach(_.remove("_id"))

    reducedMovieDocs.foreach(
      doc => {
        doc.append("movieIdx", counter.toLong)
        counter += 1


        val yearString = doc.get("year", classOf[String])
        val year =
          if (yearString == "")
            "0000".toInt
          else
            yearString.replaceAll("[^\\d.]", "").toInt

        doc.remove("year")
        doc.append("year", year)
      }
    )

    if (reducedMovieDocs.isEmpty) None
    else Some(CollectionBuildSaveRequest(reducedMovieDocs.asJava))
  }

  def getSaveRequests(
                      n: Int
                    ): List[CollectionBuildSaveRequest] = {
    var saveRequests: List[CollectionBuildSaveRequest] = List()

    while (saveRequests.isEmpty && ! queryWorkDone) {
      saveRequests =
        (0 until n)
          .toList
          .map(x => getSaveRequestOption())
          .filter(_.isDefined)
          .map(_.get)
      queryWorkDone = !readIterator.hasNext
    }
    saveRequests
  }

  def receive: PartialFunction[Any, Unit] = {
    case CollectionBuildRequest =>
      if (queryWorkDone) throw new ExceptionInInitializerError("File empty!")
      val saveRequests = getSaveRequests(numProcessors)
      queryWorkDone = !readIterator.hasNext
      saveRequests.foreach(collectionSaverActorRef ! _)
      numSendNoResponse += saveRequests.size
    case CollectionBuildSaveResult("Finished") =>
      numSendNoResponse -= 1
      if (queryWorkDone) {
        if (0 == numSendNoResponse) {
          context.parent ! CollectionBuildResult(reducedMoviesCollectionName, "Finished")
        }
      }
      else {
        val saveRequests = getSaveRequests(1)
        queryWorkDone = !readIterator.hasNext
        saveRequests.foreach(collectionSaverActorRef ! _)
        numSendNoResponse += saveRequests.size
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}

