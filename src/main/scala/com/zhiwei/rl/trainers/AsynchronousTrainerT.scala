package com.zhiwei.rl.trainers

import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.rl.networks.Network

trait AsynchronousTrainerT[Action] extends TrainerT {
  var globalSharedCounter: Long = 0

  val onlineNetwork: Network
  var targetNetwork: Network

  val rLConfig: RLConfigT
  val networkConfig: NetworkConfigT
}