package com.zhiwei.configs.rlconfigs

trait RLConfigT {
  val epsilon: Double
  val targetNetworkUpdateFactor: Int
  val onlineNetworkSaveFactor: Int
  val gamma: Double
  val batchSize: Int
  val N: Int
}
