package com.zhiwei.rl.agents.movielens

import akka.actor.{ActorRef, Props}

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection

import org.bson.Document

import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.environments.movielens.ItemBasedDQNMovieLensEnvironment
import com.zhiwei.rl.agents.AbstractAgent
import com.zhiwei.rl.agents.AbstractAgent.{LearnRequest, LearnResult}
import com.zhiwei.rl.trainers.dqntrainers.ItemBasedDQNMovieLensTrainerActor.{ChangeEpsilonRequest, FitPolicyRequest, FitPolicySuccess, GetNextActionRequest, GetNextActionResult, ReplayQueueEnqueueRequest, ReplayQueueGetBatchRequest, ReplayQueueGetBatchResult}
import com.zhiwei.types.rltypes.movielens.ItemBasedDQNMovieLensRLType._
import com.zhiwei.utils.throwNotImplementedError

object ItemBasedDQNMovieLensAgent {
  def props(
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
             trainerActorRef: ActorRef,
           ): Props = {
    Props(
      new ItemBasedDQNMovieLensAgent(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        trainerActorRef,
      )
    )
  }
}

class ItemBasedDQNMovieLensAgent(
                           rLConfig: RLConfigT,
                           override val recSysConfig: RecSysConfigT,
                           dataSetMacro: MovieLensDataSetMacro,
                           override val rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
                           trainerActorRef: ActorRef,
                )
  extends AbstractAgent[Action] {

  // Environment
  val dbName: String = dataSetMacro.dBName
  val reducedRatingsCollectionName: String = dataSetMacro.reducedRatingsCollectionName
  val movieFeaturesCollectionName: String = dataSetMacro.movieFeaturesCollectionName
  val reducedMoviesCollectionName: String = dataSetMacro.reducedMoviesCollectionName
  val client: MongoClient = new MongoClient()
  val reducedRatingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(reducedRatingsCollectionName)
  val movieFeaturesCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(movieFeaturesCollectionName)
  val reducedMoviesCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(reducedMoviesCollectionName)

  val env = new ItemBasedDQNMovieLensEnvironment(
    reducedRatingsCollection,
    movieFeaturesCollection,
    dataSetMacro.defaultObservation,
    dataSetMacro.defaultObservationArrayList
  )

  var epsilon: Double = rLConfig.epsilon

  def receive: Receive = {
    case LearnRequest =>
      initEnv()
      trainerActorRef ! GetNextActionRequest(currentState)
    case GetNextActionResult(action, recommendedMovieIndexes) =>
      val (restHistory, nextObservation, rating, envDone) = env.step(action)
      done = envDone

      val actionMovieIds: List[Long] =
        List(
          reducedMoviesCollection
            .find(new Document("movieIdx", action))
            .iterator()
            .next()
            .get("movieId", classOf[java.lang.Long])
            .toLong
        )

      val recommendedMovieIds: List[Long] =
        recommendedMovieIndexes
          .map(
            movieIdx =>
            reducedMoviesCollection
              .find(new Document("movieIdx", movieIdx))
              .iterator()
              .next()
              .get("movieId", classOf[java.lang.Long])
              .toLong
          )

      val (numRelevant, numRecommendations, numUnvisited): (Int, Int, Int) =
        getRecSysParameters(
          recommendedMovieIds,
          restHistory,
          recSysConfig.ratingThreshold
        )
      sumRelevant += numRelevant
      sumRecommendations += numRecommendations
      sumUnvisited += numUnvisited

      val reward: Reward = rLMacro.rewardFunction(actionMovieIds, restHistory)
      val nextState: State = rLMacro.stateEncodeFunction(currentState, nextObservation, rating, recSysConfig.ratingThreshold)
      val transition: Transition = (currentState, action, reward, nextState)
      trainerActorRef ! ReplayQueueEnqueueRequest(transition)
      trainerActorRef ! ReplayQueueGetBatchRequest(rLConfig.batchSize)
      currentState = nextState

      println(
        s"reward: $reward, action: $action, done: $done, episode: $episode, epsilon: $epsilon, " +
          s"train loss: $trainLoss, precision: $lastPrecision, recall: $lastRecall, maxPrecision: $maxPrecision," +
          s"maxRecall: $maxRecall, averagePrecision: $averagePrecision, averageRecall: $averageRecall," +
          s" medianPrecision: $medianPrecision, medianRecall: $medianRecall"
      )
    case ReplayQueueGetBatchResult(batchOption: Option[Transitions]) =>
      batchOption match {
        case Some(batch: Transitions) =>
          val states: List[State] = batch.map(_._1)
          val actions: List[Action] = batch.map(_._2)
          val rewards: List[Reward] = batch.map(_._3)
          val nextStates: List[State] = batch.map(_._4)

          trainerActorRef ! FitPolicyRequest(states, actions, rewards, nextStates)
        case None =>
          if (done) initEnv()
          trainerActorRef ! GetNextActionRequest(currentState)
        case msg => throwNotImplementedError(msg, self.toString())
      }
    case FitPolicySuccess(newTrainLoss: Double) =>
      trainLoss = newTrainLoss
      //      epsilon = 0.05
      epsilon = scala.math.max(epsilon * 0.999, 0.05)
      trainerActorRef ! ChangeEpsilonRequest(epsilon)
      if (done) initEnv()
      trainerActorRef ! GetNextActionRequest(currentState)
    case msg => throwNotImplementedError(msg.toString, "Agent")
  }
}