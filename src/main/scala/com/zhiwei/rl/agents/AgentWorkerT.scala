package com.zhiwei.rl.agents

import akka.actor.ActorRef
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.rl.environments.EnvironmentT
import com.zhiwei.types.rltypes.RLBaseTypeT.{Reward, State}

trait AgentWorkerT[Action] {

  val recSysConfig: RecSysConfigT

  val trainerActorRef: ActorRef

  // RecSys parameters
  var maxPrecision: Double = 0
  var maxPrecisionRecall: Double = 0
  var maxPrecisionF1Score: Double = 0
  var maxPrecisionTimeStamp: Long = 0

  var maxRecall: Double = 0
  var maxRecallPrecision: Double = 0
  var maxRecallF1Score: Double = 0
  var maxRecallTimeStamp: Long = 0

  var maxF1Score: Double = 0
  var maxF1ScorePrecision: Double = 0
  var maxF1ScoreRecall: Double = 0
  var maxF1ScoreTimeStamp: Long = 0

  // Training statistics
  var trainLoss: Double = 0

  // RL parameters
  var localEpisode: Int = 0

  var step: Int = _

  var currentState: State = _
  var done: Boolean = _

  var reverseStateHistory: List[State] = List()
  var reverseActionHistory: List[Action] = List()
  var reverseRewardHistory: List[Reward] = List()

  // Environment
  val env: EnvironmentT

  def initRecSysMetric(): Unit = {
    reverseStateHistory = List()
    reverseActionHistory = List()
    reverseRewardHistory = List()

    // RecSys parameters
    maxPrecision = 0
    maxPrecisionRecall = 0
    maxPrecisionF1Score = 0
    maxPrecisionTimeStamp = 0

    maxRecall = 0
    maxRecallPrecision = 0
    maxRecallF1Score = 0
    maxRecallTimeStamp = 0

    maxF1Score = 0
    maxF1ScorePrecision = 0
    maxF1ScoreRecall = 0
    maxF1ScoreTimeStamp = 0
  }

  def updateSingleStepRecSysMetric(
                                    numRelevant: Int,
                                    numUnvisited: Int,
                                    timeStamp: Long
                                   ): Unit = {
    val precision: Double = numRelevant * 1.0 / recSysConfig.numRecommendations
    val recall: Double = numRelevant * 1.0 / numUnvisited
    val f1Score: Double = 2 * precision * recall / (precision + recall)

    if (precision > maxPrecision) {
      maxPrecision = precision
      maxPrecisionRecall = recall
      maxPrecisionF1Score = f1Score
      maxPrecisionTimeStamp = timeStamp
    }

    if (recall > maxRecall) {
      maxRecall = recall
      maxRecallPrecision = precision
      maxRecallF1Score = f1Score
      maxRecallTimeStamp = timeStamp
    }

    if (f1Score > maxF1Score) {
      maxF1Score = f1Score
      maxF1ScorePrecision = precision
      maxF1ScoreRecall = recall
      maxF1ScoreTimeStamp = timeStamp
    }
  }


}
