package com.zhiwei.types

import com.zhiwei.types.datasettypes.movielens.{MovieLensDataSetBaseType, MovieLensDataSetBaseTypeT}
import com.zhiwei.types.dbtypes.{DBBaseType, DBBaseTypeT}
import com.zhiwei.types.recsystypes.{RecSysBaseType, RecSysBaseTypeT}
import com.zhiwei.types.rltypes.RLTypeT
import com.zhiwei.types.rltypes.movielens.ItemBasedDQNMovieLensRLType

object ItemBasedDQNMovieLensType {
  val dataSetType: MovieLensDataSetBaseTypeT = MovieLensDataSetBaseType
  val dBType: DBBaseTypeT = DBBaseType
  val recSysType: RecSysBaseTypeT = RecSysBaseType
  val rLType: RLTypeT[Int] = ItemBasedDQNMovieLensRLType
}