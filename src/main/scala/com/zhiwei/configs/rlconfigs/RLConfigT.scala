package com.zhiwei.configs.rlconfigs

trait RLConfigT {
  val epsilon: Double
  val gamma: Double
  val learningRate: Double
  val batchSize: Int
  val N: Int
}
