package com.zhiwei.types.rltypes.movielens

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType
import com.zhiwei.types.rltypes.DQNTypeT

object ItemBasedDQNMovieLensRLType extends DQNTypeT {
  type EnvStepReturn = (History, Observation, MovieLensDataSetBaseType.Rating, Done)
}

