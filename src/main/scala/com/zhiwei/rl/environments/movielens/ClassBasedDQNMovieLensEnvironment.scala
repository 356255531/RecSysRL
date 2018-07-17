package com.zhiwei.rl.environments.movielens

import com.mongodb.client.MongoCollection
import org.bson.Document
import com.zhiwei.types.rltypes.movielens.ClassBasedDQNMovieLensRLType.{Observation,Action, EnvStepReturn}
import com.zhiwei.utils.convertMongoCursor2Anys

class ClassBasedDQNMovieLensEnvironment(
                                         reducesRatingsCollection: MongoCollection[Document],
                                         movieClassFeaturesCollection: MongoCollection[Document],
                                         numRecommendations: Int,
                                         defaultObservation: Observation,
                                         override val defaultObservationArrayList: java.util.ArrayList[Double]
                          )
  extends AbstractMovieLensEnvironment[Action](
                                                      reducesRatingsCollection,
                                                      movieClassFeaturesCollection,
                                                      defaultObservation
  ) {
  def step(action: Action): EnvStepReturn = {
    val (unvisitedRecords, nextObservation, rating, done) = super.step()

    val movieDocsIterator =
      movieClassFeaturesCollection
        .find(new Document("class", action))
        .iterator

    val movieDocs = convertMongoCursor2Anys(movieDocsIterator)

    val recommendationDocs =
      scala
        .util
        .Random
        .shuffle(movieDocs)
        .take(numRecommendations)

    (recommendationDocs, unvisitedRecords, nextObservation, rating, done)
  }
}
