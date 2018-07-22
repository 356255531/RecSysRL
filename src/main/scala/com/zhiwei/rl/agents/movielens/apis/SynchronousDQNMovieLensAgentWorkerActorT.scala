package com.zhiwei.rl.agents.movielens.apis

import com.zhiwei.rl.trainers.movielens.apis.SynchronousMovieLensDQNTrainerActorT._
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.{Action, Reward, State, Transition}
import com.zhiwei.utils.throwNotImplementedError
import org.nd4j.linalg.factory.Nd4j

trait SynchronousDQNMovieLensAgentWorkerActorT extends MovieLensAgentWorkerActorT {
  def receive: Receive = {
    case LearnRequest =>
      initEnv()
      trainerActorRef ! GetNextActionAndRecommendedMovieIndexesRequest(currentState)
    case GetNextActionAndRecommendedMovieIndexesResult(action: Action, recommendedMovieIndexes) =>
      if (done) {
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

        initEnv()
        trainerActorRef ! GetNextActionAndRecommendedMovieIndexesRequest(currentState)
      }
      else {
        val (restHistory, nextObservation, rating, envDone, timeStamp) = env.step(action)
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
        val transition: Transition = (currentState.toDoubleMatrix, action, reward, nextState.toDoubleMatrix)

        reverseStateHistory = currentState :: reverseStateHistory
        reverseRewardHistory = reward :: reverseRewardHistory
        reverseActionHistory = action :: reverseActionHistory

        trainerActorRef ! ReplayQueueEnqueueRequest(transition)
        trainerActorRef ! ReplayQueueGetBatchRequest(rLConfig.batchSize)
        currentState = nextState

        val stepResult =
          StepLearnResult(
            self.toString(),
            step,
            reward,
            action,
            done,
            localEpisode,
            trainLoss
          )
        trainerActorRef ! stepResult
      }
    case ReplayQueueGetBatchResult(batchOption: Option[List[Transition]]) =>
      batchOption match {
        case Some(batch: List[Transition]) =>
          val states: List[State] = batch.map(transition => Nd4j.create(transition._1))
          val actions: List[Action] = batch.map(_._2)
          val rewards: List[Reward] = batch.map(_._3)
          val nextStates: List[State] = batch.map(transition => Nd4j.create(transition._1))

          trainerActorRef ! FitPolicyRequest(states, actions, rewards, nextStates)
        case None =>
          trainerActorRef ! GetNextActionAndRecommendedMovieIndexesRequest(currentState)
        case msg => throwNotImplementedError(msg, self.toString())
      }
    case FitPolicySuccess(newTrainLoss: Double) =>
      trainLoss = newTrainLoss
      trainerActorRef ! GetNextActionAndRecommendedMovieIndexesRequest(currentState)
    case msg => throwNotImplementedError(msg.toString, self.toString())
  }
}