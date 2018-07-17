package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}

import com.mongodb.client.{MongoCollection, MongoCursor}
import com.mongodb.MongoClient

import org.bson.Document

import scala.collection.JavaConverters._

import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.datasets.AbstractWorker
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.utils.convertMongoCursor2Anys

object EnhancedMoviesCollectionQueryWorker {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             documentReaderActorRef: ActorRef
           ): Props =
    Props(
      new EnhancedMoviesCollectionQueryWorker(
        dataSetMacro,
        documentReaderActorRef
      )
    )
}

class EnhancedMoviesCollectionQueryWorker(
                                           dataSetMacro: MovieLensDataSetMacro,
                                           documentReaderActorRef: ActorRef
                                       )
  extends AbstractWorker(dataSetMacro.movieFeaturesCollectionName) {
  val dBName: String = dataSetMacro.dBName
  val genomeScoresCollectionName: String = dataSetMacro.genomeScoresCollectionName
  val reducedRatingsCollectionName: String = dataSetMacro.reducedRatingsCollectionName

  val client = new MongoClient()
  val genomeScoresCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(genomeScoresCollectionName)
  val reducedRatingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(reducedRatingsCollectionName)

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getAverageRating(docs: List[Document]): Double = {
    val ratings = docs.map(_.get("rating", 2.5))
    ratings.foldLeft(0.0)(_ + _) / ratings.size
  }

  def getDocsToSave(movieDocs: Documents): Documents = {
    val movieIds = movieDocs.map(_.get("movieId", classOf[java.lang.Long]).toLong)
    val movieIdQueries = movieIds.map(new Document("movieId", _))

    val ratingDocsIterators: List[MongoCursor[Document]] =
      movieIdQueries.map(reducedRatingsCollection.find(_).iterator)
    val ratingDocsPerMovie: List[Documents] =
      ratingDocsIterators.map(convertMongoCursor2Anys)

    val numRatings: List[Int] = ratingDocsPerMovie.map(_.size)

    val averageRatings: List[Double] = ratingDocsPerMovie.map(getAverageRating)

    val genomeScoreDocsIteratorPerUser: List[MongoCursor[Document]] =
      movieIdQueries
        .map(
          genomeScoresCollection
            .find(_)
            .sort(new Document("tagId", 1))
            .iterator()
        )
    val genomeScoreDocsPerUser: List[Documents] =
      genomeScoreDocsIteratorPerUser.map(convertMongoCursor2Anys)
    val genomeScorePerUser: List[Document] =
      genomeScoreDocsPerUser
          .map(
            genomeScoreDocs => {
              val doc = new Document()
              genomeScoreDocs
                .foreach(
                  genomeScoreDoc =>
                    doc.append(
                      genomeScoreDoc.get("tagId", classOf[java.lang.Long]).toString,
                      genomeScoreDoc.get("relevance", classOf[java.lang.Double]).toDouble
                    )
                )
              doc
            }
          )

    movieDocs
      .indices
      .map(
        idx => {
          val doc = new Document(movieDocs(idx))
          doc.append("numRating", numRatings(idx).toLong)
          doc.append("averageRating", averageRatings(idx))
          doc.append("genomeScores", genomeScorePerUser(idx))
          doc
        }
      )
      .toList
  }

  def receive: PartialFunction[Any, Unit] = {
    case WorkRequest(movieDocs: Documents)=>
      val docToSave = getDocsToSave(movieDocs)
      documentReaderActorRef ! WorkResult(docToSave.asJava)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}