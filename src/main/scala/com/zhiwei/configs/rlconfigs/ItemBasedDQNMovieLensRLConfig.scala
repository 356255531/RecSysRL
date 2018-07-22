package com.zhiwei.configs.rlconfigs

object ItemBasedDQNMovieLensRLConfig extends RLConfigT{
  val epsilon = 1.0
  val gamma = 0.9
  val batchSize = 32
  val N = 20

  val targetNetworkUpdateFactor = 10
  val onlineNetworkSaveFactor = 100
}