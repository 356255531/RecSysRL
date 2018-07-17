package com.zhiwei.rl.environments

import com.mongodb.MongoClient

import com.zhiwei.configs.recsysconfigs
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro
import com.zhiwei.rl.environments.movielens.ClassBasedDQNMovieLensEnvironment

object ClassBasedDQNMovieLensEnvironmentTest extends App {

  val dataSetMacro = MovieLensLargeDataSetMacro
  val recSysConfig = recsysconfigs.Config_1

  val client = new MongoClient()
  val reducedRatingsCollectionName = dataSetMacro.reducedRatingsCollectionName
  val movieFeaturesCollectionName = dataSetMacro.movieFeaturesCollectionName

  val reducedRatingsCollection =
    client
    .getDatabase(dataSetMacro.dBName)
    .getCollection(reducedRatingsCollectionName)
  val movieFeaturesCollection =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(movieFeaturesCollectionName)

  val defaultObservation =
    dataSetMacro.defaultObservation

  val defaultObservationArrayList =
    dataSetMacro.defaultObservationArrayList

  val env =
    new ClassBasedDQNMovieLensEnvironment(
      reducedRatingsCollection,
      movieFeaturesCollection,
      recSysConfig.numRecommendations,
      defaultObservation,
      defaultObservationArrayList
    )

  while (true) {
    val initObservation = env.init()
    println(s"init observation $initObservation")
    var done = false
      while (!done) {
        val action = 2
        val tuple = env.step(action)
        done = tuple._5
        println(s"action $action, observation: ${tuple._3}, rating: ${tuple._4}")
      }
    }


  client.close()
}
