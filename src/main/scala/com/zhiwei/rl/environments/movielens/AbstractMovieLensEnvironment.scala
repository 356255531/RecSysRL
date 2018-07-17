package com.zhiwei.rl.environments.movielens

import org.bson.Document
import com.mongodb.client.{MongoCollection, MongoCursor}
import com.zhiwei.rl.environments.EnvironmentT

import scala.collection.JavaConverters._
import com.zhiwei.types.rltypes.RLBaseType.Observation
import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.types.rltypes.RLBaseType.Done
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.Rating
import org.nd4j.linalg.factory.Nd4j
import com.zhiwei.utils.convertMongoCursor2Anys
import org.nd4j.linalg.api.ndarray.INDArray

abstract class AbstractMovieLensEnvironment[Action](
                                              reducesRatingsCollection: MongoCollection[Document],
                                              movieClassFeaturesCollection: MongoCollection[Document],
                                              defaultObservation: Observation
                                            )
  extends EnvironmentT[Action] {

  val defaultObservationArrayList: java.util.ArrayList[Double]
  // buffer
  var numStep = 0

  var done: Boolean = true

  var userIdIterator: MongoCursor[java.lang.Long] =
    reducesRatingsCollection.distinct("movieId", classOf[java.lang.Long]).iterator()
  val userIds: List[Long] =  convertMongoCursor2Anys(userIdIterator).map(_.toLong)

  var ratingDocIterator: Iterator[Document] = _

  def getStep: Int = numStep

  def init(): Observation = {
    numStep = 0
    done = false

//    val userId = 25

    val userId = userIds(scala.util.Random.nextInt(userIds.size))

    val ratingDocMongoCursor: MongoCursor[Document] =
      reducesRatingsCollection
        .find(new Document("userId", userId))
        .sort(new Document("timestamp", 1))
        .iterator()
    val ratingDocs: Documents = convertMongoCursor2Anys(ratingDocMongoCursor)

    ratingDocIterator = ratingDocs.toIterator

    println("Environment inits succ!")

    defaultObservation
  }

  def convertRating2ContentFeature(rating: Double): INDArray = {
    Nd4j.create(Array[Double](rating / 5))
  }

  def step(): (Documents, Observation, Rating, Done) = {
    if (done) throw new IllegalStateException("Environment need to be init!")

    numStep += 1

    val unvisitedRatingRecords: Documents = ratingDocIterator.toList
    ratingDocIterator = unvisitedRatingRecords.iterator

    val ratingRecord: Document = ratingDocIterator.next
    val movieId: Long = ratingRecord.get("movieId", classOf[java.lang.Long]).toLong
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
            .get("featureVector", defaultObservationArrayList)
            .asScala
            .toArray
        )
      )

    done = !ratingDocIterator.hasNext

    (unvisitedRatingRecords, nextObservation, rating, done)
  }
}