package com.zhiwei.rl.environments

import com.mongodb.client.{MongoCollection, MongoCursor}

import org.bson.Document

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import scala.collection.JavaConverters._

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.{MovieId, Rating, TimeStamp}
import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.types.rltypes.RLBaseTypeT.{Done, Observation}
import com.zhiwei.utils.convertMongoCursor2Anys

class MovieLensEnvironment(
                                                     reducedRatingsCollection: MongoCollection[Document],
                                                     reducedMoviesCollection: MongoCollection[Document],
                                                     movieClassFeaturesCollection: MongoCollection[Document],
                                                     defaultObservation: Observation
                                                   )
  extends EnvironmentT {

  // buffer
  var numStep = 0

  var done: Boolean = true

  var userIdIterator: MongoCursor[java.lang.Integer] =
    reducedRatingsCollection.distinct("userId", classOf[java.lang.Integer]).iterator()
  val userIds: List[Int] =  convertMongoCursor2Anys(userIdIterator).map(_.toInt)

  var ratingDocIterator: Iterator[Document] = _

  val random = new scala.util.Random()

  def getStep: Int = numStep

  def init(): Observation = {
    numStep = 0
    done = false

    val userId = userIds(random.nextInt(userIds.size))

    val ratingDocMongoCursor: MongoCursor[Document] =
      reducedRatingsCollection
        .find(new Document("userId", userId))
        .sort(new Document("timestamp", 1))
        .iterator()
    val ratingDocs: Documents = convertMongoCursor2Anys(ratingDocMongoCursor)

    ratingDocs
        .foreach(
          ratingDoc => {
            val movieId: Int = ratingDoc.get("movieId", classOf[java.lang.Integer]).toInt
            val movieIdx: Int =
              reducedMoviesCollection
                .find(new Document("movieId", movieId))
                .iterator()
                .next()
                .get("movieIdx", classOf[java.lang.Integer])
                .toInt
            ratingDoc.append("movieIdx", movieIdx)
          }

        )

    ratingDocIterator = ratingDocs.toIterator

    println("Environment inits succ!")

    defaultObservation
  }

  def step(): (Documents, Observation, Rating, Done, TimeStamp) = {
    def convertRating2ContentFeature(rating: Double): INDArray = {
      Nd4j.create(Array[Double](rating / 5))
    }

    if (done) throw new IllegalStateException("Environment need to be init!")

    numStep += 1

    val unvisitedRatingRecords: Documents = ratingDocIterator.toList
    ratingDocIterator = unvisitedRatingRecords.iterator

    val ratingRecord: Document = ratingDocIterator.next
    val timestamp: Long = ratingRecord.get("timestamp", classOf[java.lang.Long]).toLong
    val movieId: MovieId = ratingRecord.get("movieId", classOf[java.lang.Integer]).toInt
    val rating: Double = ratingRecord.get("rating", classOf[java.lang.Double]).toDouble
    val nextObservation =
      Nd4j.concat(
        1,
        convertRating2ContentFeature(rating),
        Nd4j.create(
          movieClassFeaturesCollection
            .find(new Document("movieId", movieId))
            .iterator()
            .next()
            .get("featureVector", classOf[java.util.ArrayList[Double]])
            .asScala
            .toArray
        )
      )

    done = !ratingDocIterator.hasNext

    (unvisitedRatingRecords, nextObservation, rating, done, timestamp)
  }

  def step[Action](action: Action): (Documents, Observation, Rating, Done, TimeStamp) = step()
}