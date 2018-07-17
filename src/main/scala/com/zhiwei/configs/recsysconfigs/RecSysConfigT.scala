package com.zhiwei.configs.recsysconfigs

trait RecSysConfigT {
  val numRecommendations: Int
  val ratingThreshold: Double
  val numPrecisionToKeep: Int
  val numRecallToKeep: Int
}
