package com.zhiwei.rl.environments.movielens

import com.mongodb.client.MongoCollection

import org.bson.Document

import com.zhiwei.types.rltypes.movielens.ItemBasedDQNMovieLensRLType.{Observation, Action, EnvStepReturn}

class ItemBasedDQNMovieLensEnvironment(
                                        reducesRatingsCollection: MongoCollection[Document],
                                        movieClassFeaturesCollection: MongoCollection[Document],
                                        defaultObservation: Observation,
                                        override val defaultObservationArrayList: java.util.ArrayList[Double]
                                      )
  extends AbstractMovieLensEnvironment[Action](
    reducesRatingsCollection,
    movieClassFeaturesCollection,
    defaultObservation
  ) {
  def step(action: Action): EnvStepReturn = super.step()
}
