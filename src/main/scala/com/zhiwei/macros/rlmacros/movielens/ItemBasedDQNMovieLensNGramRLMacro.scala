package com.zhiwei.macros.rlmacros.movielens

import com.zhiwei.macros.rlmacros.RLMacroT
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.{Rating, RatingThreshold}
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieId
import com.zhiwei.types.rltypes.DQNType.{History, Observation, Reward, State}
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

class ItemBasedDQNMovieLensNGramRLMacro(
               numDefaultObservationEntry: Int,
               n: Int,
               stateEncodeFunctionBar: (State, Observation, Rating, RatingThreshold) => State,
               rewardFunctionBar: (List[MovieId], History) => Reward
               ) extends RLMacroT {
    val defaultState: INDArray =
    Nd4j.create(Array.fill[Double](n, numDefaultObservationEntry)(0))

  def stateEncodeFunction: (State, Observation, Rating, RatingThreshold) => State = stateEncodeFunctionBar

  def rewardFunction: (List[MovieId], History) => Reward = rewardFunctionBar
}