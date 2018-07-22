package com.zhiwei.rl.agents.movielens.apis

import akka.actor.{Actor, ActorLogging}
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.RLMacroT
import com.zhiwei.rl.agents.AgentWorkerT
import com.zhiwei.rl.environments.MovieLensEnvironment
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.{MovieIdx, RatingThreshold}
import com.zhiwei.types.rltypes.DQNType.Action
import com.zhiwei.types.rltypes.RLBaseTypeT.History
import org.bson.Document

trait MovieLensAgentWorkerActorT extends AgentWorkerT[Action] with Actor with ActorLogging{

  val rLConfig: RLConfigT
  val recSysConfig: RecSysConfigT
  val dataSetMacro: MovieLensDataSetMacro
  val rLMacro: RLMacroT

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

  def getRewardRecommendedMovieIndexes(action: Action): List[MovieIdx]

  override def preStart(): Unit =
    log.info("Agent worker starts.")

  override def postStop(): Unit = {
    log.info("Agent worker ends.")
    client.close()
  }

  def initEnv(): Unit = {
    env.init()

    step = 0
    localEpisode += 1
    currentState = rLMacro.defaultState
    done = false

    initRecSysMetric()
  }

  def getMovieLensRecSysParameters(
                           recommendedMovieIndexes: List[MovieIdx],
                           restHistory: History,
                           ratingThreshold: RatingThreshold
                         ): (Int, Int) = {
    val recommendedMovieIdxSet: Set[MovieIdx] = recommendedMovieIndexes.toSet

    val restRelevantMovieIdxSet: Set[MovieIdx] =
      restHistory
        .filter(
          _.get("rating", classOf[java.lang.Double]).toDouble > ratingThreshold
        )
        .map(_.get("movieIdx", classOf[java.lang.Integer]).toInt)
        .toSet

    val numRelevant: Int = (recommendedMovieIdxSet intersect restRelevantMovieIdxSet).size
    val numUnvisited: Int = restHistory.size

    (numRelevant, numUnvisited)
  }
}