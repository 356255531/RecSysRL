package com.zhiwei.configs.rlconfigs

object ItemBasedDQNMovieLensRLConfig extends RLConfigT{
  val epsilon = 1.0
  val gamma = 0.99
  val learningRate = 0.001
  val batchSize = 1028
  val N = 20
}