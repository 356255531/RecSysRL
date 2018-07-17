package com.zhiwei.rl.environments

import com.mongodb.MongoClient
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro
import com.zhiwei.rl.environments.movielens.ContinuousActionMovieLensEnvironment
import org.bson.Document

object ContinuousActionMovieLensEnvironmentTest extends App {

  val dataSetMacro = MovieLensLargeDataSetMacro

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
    new ContinuousActionMovieLensEnvironment(
      reducedRatingsCollection,
      movieFeaturesCollection,
      defaultObservation,
      defaultObservationArrayList
    )

  while (true) {
    val initObservation = env.init()
    println(s"init observation $initObservation")
    var done = false
      while (!done) {
        val action: Int = 1
        val tuple = env.step(List(new Document()))
        done = tuple._4
        println(s"action $action, observation: ${tuple._2}, rating: ${tuple._3}")
      }
    }


  client.close()
}
