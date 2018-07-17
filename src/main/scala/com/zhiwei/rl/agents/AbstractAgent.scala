package com.zhiwei.rl.agents

import akka.actor.{Actor, ActorLogging}

import scala.collection.immutable
import scala.math.max

import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.macros.rlmacros.RLMacroT
import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.types.rltypes.RLBaseType.{History, State}
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.RatingThreshold
import com.zhiwei.rl.environments.EnvironmentT
import com.zhiwei.utils.queue2FiniteQueue

object AbstractAgent {
  final case object LearnRequest
  final case object LearnResult
  final case class StepLearnResult(
                                    currentState: Any,
                                    action: Any,
                                    nextState:Any,
                                    reward: Any
                                  )
}

abstract class AbstractAgent[Action]
  extends Actor with ActorLogging {

  val rLMacro: RLMacroT
  val recSysConfig: RecSysConfigT

  // Training statistics
  var trainLoss: Double = _

  // RecSys parameters
  var lastPrecision: Double = _
  var lastRecall: Double = _

  var maxPrecision: Double = _
  var maxRecall: Double = _

  var averagePrecision: Double = _
  var averageRecall: Double = _

  var medianPrecision: Double = _
  var medianRecall: Double = _

  var sumRelevant: Long  = 0
  var sumRecommendations: Long  = 0
  var sumUnvisited: Long  = 0

  var recallHistory: List[Double] = List()
  var precisionHistory: List[Double] = List()

  // RL parameters
  var episode: Double = 0

  // Buffer data
  var currentState: State = _
  var done: Boolean = _

  val env: EnvironmentT[Action]

  def initEnv(): Unit = {
    env.init()

    lastPrecision = sumRelevant / max(sumRecommendations, 1.0)
    lastRecall = sumRelevant / max(sumUnvisited, 1.0)

    maxPrecision = max(maxPrecision, lastPrecision)
    maxRecall = max(maxRecall, lastRecall)

    sumRelevant = 0
    sumRecommendations = 0
    sumUnvisited = 0

    recallHistory = lastRecall :: recallHistory.takeRight(recSysConfig.numRecallToKeep - 1)
    precisionHistory = lastPrecision :: precisionHistory.takeRight(recSysConfig.numPrecisionToKeep - 1)

    averagePrecision = precisionHistory.foldLeft(0.0)(_ + _) / max(precisionHistory.size, 1.0)
    averageRecall = recallHistory.foldLeft(0.0)(_ + _) / max(recallHistory.size, 1.0)

    val medianPrecisionOption =
      precisionHistory.sortWith(_ < _).drop(precisionHistory.length / 2).headOption
    medianPrecision = if (medianPrecisionOption.isDefined) medianPrecisionOption.get else 0
    val medianRecallOption =
      recallHistory.sortWith(_ < _).drop(recallHistory.length / 2).headOption
    medianRecall = if (medianRecallOption.isDefined) medianRecallOption.get else 0

    episode += 1

    done = false

    currentState = rLMacro.defaultState
  }

  def getRecSysParameters(
                           recommendedMovieIds: List[Long],
                           restHistory: History,
                           ratingThreshold: RatingThreshold
                         ): (Int, Int, Int) = {
    val recommendedMovieIdSet: Set[Long] = recommendedMovieIds.toSet

    val restRelevantMovieIdSet: Set[Long] =
      restHistory
        .filter(
          _.get("rating", classOf[java.lang.Double]).toDouble > ratingThreshold
        )
        .map(_.get("movieId", classOf[java.lang.Long]).toLong)
        .toSet

    val numRelevant: Int = (recommendedMovieIdSet intersect restRelevantMovieIdSet).size
    val numRecommendations: Int = recommendedMovieIdSet.size
    val numUnvisited: Int = restHistory.size

    (numRelevant, numRecommendations, numUnvisited)
  }
}
