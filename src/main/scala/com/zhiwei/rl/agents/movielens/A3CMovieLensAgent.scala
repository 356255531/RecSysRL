package com.zhiwei.rl.agents.movielens

import akka.actor.{ActorRef, Props}

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection

import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ContinuousMovieLensRLMacro
import com.zhiwei.rl.agents.AbstractAgent
import com.zhiwei.rl.agents.AbstractAgent.LearnRequest
import com.zhiwei.rl.environments.movielens.ContinuousActionMovieLensEnvironment
import com.zhiwei.rl.policys.AsynchronousACPolicyT
import com.zhiwei.rl.policys.AsynchronousACPolicyT.{ApplyGradientRequest, GetACPolicyRequest, GlobalCounterIncreaseRequest}
import com.zhiwei.rl.policys.movielens.ActorMovieLensPolicy.GetACPolicyResult
import com.zhiwei.types.rltypes.movielens.ContinuousActionMovieLensRLType.{Action, Reward, State}
import com.zhiwei.utils.throwNotImplementedError
import org.bson.Document

object A3CMovieLensAgent {
  def props(
             rLConfig: RLConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             trainerActorRef: ActorRef,
             recSysConfig: RecSysConfigT,
             rLMacro: ContinuousMovieLensRLMacro
           ): Props = {
    Props(
      new A3CMovieLensAgent(
        rLConfig,
        dataSetMacro,
        trainerActorRef,
        recSysConfig,
        rLMacro
      )
    )
  }
}

class A3CMovieLensAgent(
                         rLConfig: RLConfigT,
                         dataSetMacro: MovieLensDataSetMacro,
                         trainerActorRef: ActorRef,
                         override val recSysConfig: RecSysConfigT,
                         override val rLMacro: ContinuousMovieLensRLMacro
                       )
  extends AbstractAgent[Action] {

  // Environment
  val dbName: String = dataSetMacro.dBName
  val reducedRatingsCollectionName: String =
    dataSetMacro.reducedRatingsCollectionName
  val movieFeaturesCollectionName: String =
    dataSetMacro.movieFeaturesCollectionName
  val client = new MongoClient()
  val reducedRatingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(reducedRatingsCollectionName)
  val movieFeaturesCollection: MongoCollection[Document] =
    client
      .getDatabase(dataSetMacro.dBName)
      .getCollection(movieFeaturesCollectionName)
  val env = new ContinuousActionMovieLensEnvironment(
    reducedRatingsCollection,
    movieFeaturesCollection,
    dataSetMacro.defaultObservation,
    dataSetMacro.defaultObservationArrayList
  )

  // Actor and Critic
  var acPolicy: AsynchronousACPolicyT[Action] = _

  // Asynchronous parameter
  var threadStepCounter: Long = 1

  var startStep: Long = _

  // Buffer
  var states: List[State] = _
  var actions: List[Action] = _
  var rewards: List[Reward] = _

  override def initEnv(): Unit = {
    super.initEnv()

    states = List()
    actions = List()
    rewards = List()

    startStep = threadStepCounter
  }

  def receive: Receive = {
    case LearnRequest =>
      initEnv()
      trainerActorRef ! GetACPolicyRequest
    case GetACPolicyResult(newAcPolicy) =>
      acPolicy = newAcPolicy
      if (!done) {
        val action = acPolicy.getNextAction(currentState)

        val (restHistory, nextObservation, rating, envDone) = env.step(action)
        done = envDone
        threadStepCounter += 1
        trainerActorRef ! GlobalCounterIncreaseRequest

        val movieIds = action.map(_.get("movieId", classOf[java.lang.Long]).toLong)
        val (numRelevant, numRecommendations, numUnvisited): (Int, Int, Int) =
          getRecSysParameters(
            movieIds,
            restHistory,
            recSysConfig.ratingThreshold
          )
        sumRelevant += numRelevant
        sumRecommendations += numRecommendations
        sumUnvisited += numUnvisited

        states = states :+ currentState
        actions = actions :+ action
        val reward: Reward = rLMacro.rewardFunction(movieIds, restHistory)
        rewards = rewards :+ reward

        println(
          s"reward: $reward, action: $action, done: $done,  train loss: $trainLoss, " +
            s"precision: $lastPrecision, recall: $lastRecall, maxPrecision: $maxPrecision," +
            s"maxRecall: $maxRecall, averagePrecision: $averagePrecision, averageRecall: $averageRecall," +
            s" medianPrecision: $medianPrecision, medianRecall: $medianRecall"
        )

        val nextState: State = rLMacro.stateEncodeFunction(currentState, nextObservation, rating, recSysConfig.ratingThreshold)
        currentState = nextState
      }
      val (policyGradient, valueGradient, batchSize) = acPolicy.getGradients(states, actions, rewards)
      trainerActorRef ! ApplyGradientRequest(policyGradient, valueGradient, batchSize)
    case msg => throwNotImplementedError(msg.toString, self.toString())
  }
}