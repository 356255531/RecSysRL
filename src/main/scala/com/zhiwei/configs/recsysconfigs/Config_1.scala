package com.zhiwei.configs.recsysconfigs

object Config_1 extends RecSysConfigT {
  val numRecommendations: Int = 10
  val ratingThreshold: Double = 2.0
  val numPrecisionToKeep: Int = 30
  val numRecallToKeep: Int = 30
}
