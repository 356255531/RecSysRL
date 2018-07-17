package com.zhiwei.types.rltypes.movielens

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType
import com.zhiwei.types.dbtypes.DBBaseType
import com.zhiwei.types.rltypes.DQNTypeT

object ClassBasedDQNMovieLensRLType extends DQNTypeT {
  type EnvStepReturn = (DBBaseType.Documents, History, Observation, MovieLensDataSetBaseType.Rating, Done)
}

