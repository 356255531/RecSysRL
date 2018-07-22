package com.zhiwei.macros.rlmacros

import com.zhiwei.types.rltypes.RLBaseTypeT.{History, Observation, Reward, State}
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.{MovieId, Rating, RatingThreshold}

trait RLMacroT {
  val defaultState: State

  def rewardFunction: (List[MovieId], History) => Reward

  def stateEncodeFunction: (State, Observation, Rating, RatingThreshold) => State
}
