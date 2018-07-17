package com.zhiwei.rl.agents.movielens

import akka.actor.ActorRef
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.AbstractAgent
import com.zhiwei.rl.agents.AbstractAgent.LearnRequest
import com.zhiwei.rl.environments.movielens.ClassBasedDQNMovieLensEnvironment
import com.zhiwei.rl.trainers.dqntrainers.ClassBasedDQNMovieLensTrainerActor._
import com.zhiwei.types.rltypes.movielens.ClassBasedDQNMovieLensRLType._
import com.zhiwei.utils.throwNotImplementedError
import org.bson.Document


class AsynchronousClassBasedDQNMovieLensAgent(
                                   rLConfig: RLConfigT,
                                   override val recSysConfig: RecSysConfigT,
                                   dataSetMacro: MovieLensDataSetMacro,
                                   override val rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
                                   trainerActorRef: ActorRef,
                )
  extends AbstractAgent[Action] {

  // Environment
  val dbName: String = dataSetMacro.dBName
  val reducedRatingsCollectionName: String =
    dataSetMacro.reducedRatingsCollectionName
  val movieClassFeaturesCollectionName: String =
    dataSetMacro.movieClassFeaturesCollectionName
  val client = new MongoClient()
  val reducedRatingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(reducedRatingsCollectionName)
  val movieClassFeaturesCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(movieClassFeaturesCollectionName)
  val env =
    new ClassBasedDQNMovieLensEnvironment(
      reducedRatingsCollection,
      movieClassFeaturesCollection,
      recSysConfig.numRecommendations,
      dataSetMacro.defaultObservation,
      dataSetMacro.defaultObservationArrayList
    )

  var epsilon: Double = rLConfig.epsilon

  def receive: Receive = {
    case LearnRequest =>
      initEnv()
      trainerActorRef ! GetNextActionRequest(currentState)
    case GetNextActionResult(action: Action) =>
      val (recommendationDocs, restHistory, nextObservation, rating, envDone) = env.step(action)
      done = envDone

      val recommendedMovieIds = recommendationDocs.map(_.get("movieId", classOf[java.lang.Long]).toLong)
      val (numRelevant, numRecommendations, numUnvisited): (Int, Int, Int) =
        getRecSysParameters(
          recommendedMovieIds,
          restHistory,
          recSysConfig.ratingThreshold
        )
      sumRelevant += numRelevant
      sumRecommendations += numRecommendations
      sumUnvisited += numUnvisited

      val reward: Reward = rLMacro.rewardFunction(recommendedMovieIds, restHistory)
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