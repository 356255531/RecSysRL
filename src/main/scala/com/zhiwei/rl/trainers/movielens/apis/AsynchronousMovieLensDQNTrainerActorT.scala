package com.zhiwei.rl.trainers.movielens.apis

import akka.actor.{Actor, ActorLogging, PoisonPill}
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.trainers.AsynchronousTrainerT
import com.zhiwei.types.rltypes.DQNType.{Action, Done, Reward}
import com.zhiwei.utils.throwNotImplementedError
import org.deeplearning4j.nn.gradient.Gradient
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import AsynchronousMovieLensDQNTrainerActorT._

object AsynchronousMovieLensDQNTrainerActorT {
  final case object TrainRequest
  final case class LearnRequest(dQNNetwork: Network)
  final case object GlobalShareCounterIncreaseRequest
  final case object GetDQNNetworkRequest
  final case class GetDQNNetworkResult(valueNetwork: Network)
  final case class PushGradientRequest(gradient: Gradient, batchSize: Int)
  final case class RVectorEvaluateRequest(
                                           reversedStateInputMatrix: INDArray,
                                           reverseActionHistory: List[Action],
                                           reverseRewardHistory: List[Reward]
                                         )
  final case class RVectorEvaluateResult(RVector: INDArray)
  final case class StepLearnResult[Action](
                                            actorName: String,
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

trait AsynchronousMovieLensDQNTrainerActorT[Action]
  extends AsynchronousTrainerT[Action] with Actor
    with ActorLogging {

  override def preStart(): Unit = {
    log.info(s"${trainerName}Actor starts.")
  }

  override def postStop(): Unit = {
    log.info(s"${trainerName}Actor ends.")
  }

  override def receive: Receive = {
    case TrainRequest =>
      (0 until numWorker)
        .foreach(
          idx =>
            workerRouterActorRef ! GetDQNNetworkResult(onlineNetwork.clone)
        )
    case RVectorEvaluateRequest(reversedStateInputMatrix, reverseActionHistory, reverseRewardHistory) =>
      val targetNetworkOutput: INDArray = targetNetwork.eval(reversedStateInputMatrix)
      val RVector: INDArray =
        Nd4j.create(
          reverseActionHistory
            .zipWithIndex
            .map(
              tuple =>
                targetNetworkOutput.getDouble(tuple._2, tuple._1)
            )
            .toArray
        ).transpose()
      val reversedRewardVector: INDArray =
        Nd4j.create(reverseRewardHistory.toArray).transpose()
      val updatedRVector: INDArray =
        reversedRewardVector.add(RVector.mul(rLConfig.gamma))
      sender ! RVectorEvaluateResult(updatedRVector)
    case GlobalShareCounterIncreaseRequest =>
      globalSharedCounter += 1
      if (globalSharedCounter % rLConfig.targetNetworkUpdateFactor == 0)
        targetNetwork = onlineNetwork.clone
    case PushGradientRequest(gradient: Gradient, batchSize: Int) =>
      onlineNetwork.applyGradient(gradient, batchSize)
    case  StepLearnResult(
                              workerActorName,
                              reward,
                              action,
                              done,
                              episode,
                              trainLoss,
    ) =>
      println(
        s"worker: $workerActorName, episode: $episode, reward: $reward, action: $action, " +
          s"done: $done, train loss: $trainLoss"
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
        onlineNetwork.save(networkConfig.fileName)

      if (ifLearnTerminated) {
        (0 until numWorker)
          .foreach(_ => workerRouterActorRef ! PoisonPill)
        context.system.stop(context.parent)
      }
      println(
        s"worker: $workerActorName, episode: $episode, stepReward: $stepReward, " +
          s"maxPrecision: $maxPrecision, maxPrecisionTimeStamp: $maxPrecisionTimeStamp," +
          s" maxRecall: $maxRecall, maxRecallTimeStamp:$maxRecallTimeStamp maxF1Score: $maxF1Score, " +
          s"maxF1ScoreTimeStamp: $maxF1ScoreTimeStamp, maxPrecisionRecall: $maxPrecisionRecall, " +
          s"maxPrecisionF1Score: $maxPrecisionF1Score, maxRecallPrecision: $maxRecallPrecision, " +
          s"maxRecallF1Score: $maxRecallF1Score, maxF1ScorePrecision: $maxF1ScorePrecision, " +
          s"maxF1ScoreRecall: $maxF1ScoreRecall"
      )
    case msg => throwNotImplementedError(msg, self.toString())
  }
}
