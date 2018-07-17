package com.zhiwei.types.rltypes.movielens

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType
import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.types.rltypes.RLTypeT


object ContinuousActionMovieLensRLType extends RLTypeT[Documents] {
  type EnvStepReturn = (History, Observation, MovieLensDataSetBaseType.Rating, Done)
}


