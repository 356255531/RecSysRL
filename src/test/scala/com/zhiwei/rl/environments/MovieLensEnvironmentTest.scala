package com.zhiwei.rl.environments

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.zhiwei.configs.datasetconfigs.{DataSetConfigT, MovieLensLargeDataSetConfig}
import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.{networkconfigs, recsysconfigs, replayqueueconfigs}
import com.zhiwei.configs.rlconfigs.{ItemBasedDQNMovieLensRLConfig, RLConfigT}
import com.zhiwei.configs.trainerconfigs.{ItemBasedDQNMovieLensTrainerConfig, TrainerConfigT}
import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
import com.zhiwei.macros.nnmacros.ItemBasedDQNMovieLensNNMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.rewardfunctions.movielens.hitMovieNotNegReward
import com.zhiwei.rl.stateencoders.NGramStateEncoder
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieId
import com.zhiwei.types.rltypes.DQNType.{History, Reward}
import org.bson.Document
import org.nd4j.linalg.api.ndarray.INDArray

object MovieLensEnvironmentTest extends App {
  val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro

  // Environment
  val dbName: String = dataSetMacro.dBName
  val reducedRatingsCollectionName: String =
    dataSetMacro.reducedRatingsCollectionName
  val reducedMoviesCollectionName: String =
    dataSetMacro.reducedMoviesCollectionName
  val movieClassFeaturesCollectionName: String =
    dataSetMacro.movieClassFeaturesCollectionName
  val client = new MongoClient()
  val reducedRatingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(reducedRatingsCollectionName)
  val reducedMoviesCollection: MongoCollection[Document] =
    client
      .getDatabase(dbName)
      .getCollection(reducedMoviesCollectionName)
  val movieClassFeaturesCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(movieClassFeaturesCollectionName)
  val env =
    new MovieLensEnvironment(
      reducedRatingsCollection,
      reducedMoviesCollection,
      movieClassFeaturesCollection,
      dataSetMacro.defaultObservation,
    )

  val dataSetConfig: DataSetConfigT = MovieLensLargeDataSetConfig
  val recSysConfig: RecSysConfigT = recsysconfigs.Config_1
  val networkConfig: NetworkConfigT = networkconfigs.SynItemBasedMovieLensDQNConfig
  val replayQueueConfig: ReplayQueueConfigT = replayqueueconfigs.Config_1
  val rLConfig: RLConfigT = ItemBasedDQNMovieLensRLConfig
  val trainerConfig: TrainerConfigT = ItemBasedDQNMovieLensTrainerConfig

  val rewardFunction: (List[MovieId], History) =>  Reward =
    hitMovieNotNegReward(recSysConfig.ratingThreshold)

  val rLMacro =
    new ItemBasedDQNMovieLensNGramRLMacro(
      dataSetMacro.numObservationEntry,
      rLConfig.N,
      NGramStateEncoder.getNextState,
      rewardFunction
    )
  val nNMacro =
    new ItemBasedDQNMovieLensNNMacro(
      networkConfig,
      dataSetMacro,
      rLConfig
    )

  while (true) {
    val initObservation = env.init()
    var state: INDArray = rLMacro.defaultState
    println(s"init observation $initObservation, init state is $state")
    var done = false
      while (!done) {
        val action: Int = 1
        val (restHistory, nextObservation, rating, envDone, timeStamp) = env.step(action)
        state = NGramStateEncoder.getNextState(state, nextObservation, rating, recSysConfig.ratingThreshold)
        println(s"after perform action: $action, the state is $state")
        done = envDone
        println(s"action $action, observation: $nextObservation, rating: $rating}, done: $done")
      }
    }


  client.close()
}
