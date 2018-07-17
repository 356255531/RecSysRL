package com.zhiwei.macros.rlmacros

import com.zhiwei.types.rltypes.RLBaseType.{History, Reward, State}
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIds

trait RLMacroT {
  val defaultState: State

  def rewardFunction: (MovieIds, History) => Reward
}
