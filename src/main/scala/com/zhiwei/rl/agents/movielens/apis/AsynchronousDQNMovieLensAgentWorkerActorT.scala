package com.zhiwei.rl.agents.movielens.apis

import com.zhiwei.rl.policys.movielens.apis.DoubleDQNMovieLensPolicyT
import com.zhiwei.rl.trainers.movielens.apis.AsynchronousMovieLensDQNTrainerActorT._
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.{Reward, State}
import com.zhiwei.utils.{convertState2NNInput, throwNotImplementedError}
import org.deeplearning4j.nn.gradient.Gradient
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

trait AsynchronousDQNMovieLensAgentWorkerActorT extends MovieLensAgentWorkerActorT {

  var reversedStateInputMatrix: INDArray = _

  val dQNPolicy: DoubleDQNMovieLensPolicyT

  def receive: Receive = {
    case GetDQNNetworkResult(onlineNetwork) =>
      dQNPolicy.updateOnlineNetwork(onlineNetwork)

      initEnv()
      while (!done) {
        val (action, recommendedMovieIndexes) = dQNPolicy.getNextActionAndRecommendedMovieIndexes(currentState)
        val (restHistory, nextObservation, rating, envDone, timeStamp) = env.step(action)
        trainerActorRef ! GlobalShareCounterIncreaseRequest
        step += 1
        done = envDone

        val (numRelevant, numUnvisited): (Int, Int) =
          getMovieLensRecSysParameters(
            recommendedMovieIndexes,
            restHistory,
            recSysConfig.ratingThreshold
          )
        updateSingleStepRecSysMetric(numRelevant, numUnvisited, timeStamp)

        val rewardMovieIndexes: List[MovieIdx] = getRewardRecommendedMovieIndexes(action)
        val reward: Reward = rLMacro.rewardFunction(rewardMovieIndexes, restHistory)
        val nextState: State = rLMacro.stateEncodeFunction(currentState, nextObservation, rating, recSysConfig.ratingThreshold)

        reverseRewardHistory = reward :: reverseRewardHistory
        reverseStateHistory = currentState :: reverseStateHistory
        reverseActionHistory = action :: reverseActionHistory

        currentState = nextState

        val stepResult =
          StepLearnResult(
            self.toString(),
            reward,
            action,
            done,
            localEpisode,
            trainLoss
          )
        trainerActorRef ! stepResult
      }

      val stepReward: Double = reverseRewardHistory.fold(0.0)(_ + _) / reverseRewardHistory.size
      val episodeLearnResult: EpisodeLearnResult =
        EpisodeLearnResult(
          self.toString(),
          localEpisode,
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
        )
      trainerActorRef ! episodeLearnResult

      reversedStateInputMatrix =
        Nd4j.concat(
          0,
          reverseStateHistory.map(convertState2NNInput):_*
        )

      val rVectorEvaluateRequest: RVectorEvaluateRequest =
        RVectorEvaluateRequest(
          reversedStateInputMatrix,
          reverseActionHistory,
          reverseRewardHistory
        )
      trainerActorRef ! rVectorEvaluateRequest
    case RVectorEvaluateResult(rVector) =>
      val gradient: Gradient = dQNPolicy.getGradient(reversedStateInputMatrix, rVector)
      val batchSize: Int = reverseStateHistory.size
      trainerActorRef ! PushGradientRequest(gradient, batchSize)
    case msg => throwNotImplementedError(msg.toString, self.toString())
  }
}