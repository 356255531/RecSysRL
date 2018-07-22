package com.zhiwei.rl.trainers.movielens.apis

import akka.actor.{Actor, ActorLogging, PoisonPill}
import com.zhiwei.rl.trainers.SynchronousTrainerT
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.Transition
import com.zhiwei.types.rltypes.RLBaseTypeT.{Done, Reward, State}
import com.zhiwei.utils.throwNotImplementedError

import SynchronousMovieLensDQNTrainerActorT._

object SynchronousMovieLensDQNTrainerActorT {
  final case object TrainRequest
  final case object LearnRequest
  final case class GetNextActionAndRecommendedMovieIndexesRequest(state: State)
  final case class GetNextActionAndRecommendedMovieIndexesResult(
                                                                  action: Any,
                                                                  recommendedMovieIndexes: List[MovieIdx]
                                                                )
  final case class ReplayQueueEnqueueRequest(transition: Any)
  final case class ReplayQueueGetBatchRequest(batchSize: Int)
  final case class ReplayQueueGetBatchResult(transition: Option[List[Any]])
  final case class FitPolicyRequest(
                                     states: List[State],
                                     actions: List[Any],
                                     rewards: List[Reward],
                                     nextStates: List[State]
                                   )
  final case class FitPolicySuccess(newTrainLoss: Double)
  final case class StepLearnResult[Action](
                                            actorName: String,
                                            step: Int,
                                            reward: Reward,
                                            action: Action,
                                            done: Done,
                                            episode: Int,
                                            trainLoss: Double,
                                          )
  final case class EpisodeLearnResult(
                                       actorName: String,
                                       episode: Int,
                                       stepReward: Double,
                                       maxPrecision: Double,
                                       maxPrecisionRecall: Double,
                                       maxPrecisionF1Score: Double,
                                       maxPrecisionTimeStamp: Long,
                                       maxRecall: Double,
                                       maxRecallPrecision: Double,
                                       maxRecallF1Score: Double,
                                       maxRecallTimeStamp: Long,
                                       maxF1Score: Double,
                                       maxF1ScorePrecision: Double,
                                       maxF1ScoreRecall: Double,
                                       maxF1ScoreTimeStamp: Long
                                     )
}

trait SynchronousMovieLensDQNTrainerActorT[Action]
  extends SynchronousTrainerT[Action, Transition] with Actor
    with ActorLogging {

  override def preStart(): Unit = {
    log.info(s"${trainerName}Actor starts.")
  }

  override def postStop(): Unit = {
    log.info(s"${trainerName}Actor ends.")
  }

  override def receive: Receive = {
    case TrainRequest =>
      workerRouterActorRef ! LearnRequest
    case GetNextActionAndRecommendedMovieIndexesRequest(state: State) =>
      val (action, recommendedMovieIndexes) = policy.getNextActionAndRecommendedMovieIndexes(state)
      sender ! GetNextActionAndRecommendedMovieIndexesResult(action, recommendedMovieIndexes)
    case ReplayQueueEnqueueRequest(transition: Transition) =>
      replayQueue.enqueueTransition(transition)
    case ReplayQueueGetBatchRequest(batchSize: Int) =>
      val transitionOption = replayQueue.getTransitionBatchOption(batchSize)
      sender ! ReplayQueueGetBatchResult(transitionOption)
    case FitPolicyRequest(
                              states: List[State],
                              actions: List[Action],
                              rewards: List[Reward],
                              nextStates: List[State]
    ) =>
      val error = policy.fit(states, actions, rewards, nextStates)
      sender ! FitPolicySuccess(error)
      epsilon = scala.math.max(epsilon * 0.99999, 0.05)
      policy.setEpsilon(epsilon)
    case  StepLearnResult(
                              workerActorName,
                              step,
                              reward,
                              action,
                              done,
                              episode,
                              trainLoss,
    ) =>
      println(
        s"worker: $workerActorName, episode: $episode, step: $step, reward: $reward, " +
          s"action: $action, done: $done, epsilon: $epsilon, train loss: $trainLoss"
      )
    case EpisodeLearnResult(
                                workerActorName,
                                episode,
                                stepReward,
                                maxPrecision,
                                maxPrecisionRecall,
                                maxPrecisionF1Score,
                                maxPrecisionTimeStamp,
                                maxRecall,
                                maxRecallPrecision,
                                maxRecallF1Score,
                                maxRecallTimeStamp,
                                maxF1Score,
                                maxF1ScorePrecision,
                                maxF1ScoreRecall,
                                maxF1ScoreTimeStamp
    ) =>
      numTrainEpisode += 1

      if (episode % rLConfig.targetNetworkUpdateFactor == 0)
        policy.updateNetwork(None, None)

      if (episode % rLConfig.onlineNetworkSaveFactor == 0)
        policy.saveNetwork(networkConfig.fileName)

      if (ifLearnTerminated) {
        (0 until numWorker)
          .foreach(_ => workerRouterActorRef ! PoisonPill)
        context.system.stop(context.parent)
      }
      println(
        s"worker: $workerActorName, episode: $episode, stepReward: $stepReward, " +
          s"epsilon: $epsilon, maxPrecision: $maxPrecision, maxPrecisionTimeStamp: $maxPrecisionTimeStamp," +
          s" maxRecall: $maxRecall, maxRecallTimeStamp:$maxRecallTimeStamp maxF1Score: $maxF1Score, " +
          s"maxF1ScoreTimeStamp: $maxF1ScoreTimeStamp, maxPrecisionRecall: $maxPrecisionRecall, " +
          s"maxPrecisionF1Score: $maxPrecisionF1Score, maxRecallPrecision: $maxRecallPrecision, " +
          s"maxRecallF1Score: $maxRecallF1Score, maxF1ScorePrecision: $maxF1ScorePrecision, " +
          s"maxF1ScoreRecall: $maxF1ScoreRecall"
      )
    case msg => throwNotImplementedError(msg, self.toString())
  }
}
