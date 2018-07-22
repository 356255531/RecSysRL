package com.zhiwei.rl.trainers

import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.rl.memories.ReplayQueue
import com.zhiwei.rl.policys.movielens.apis.MovieLensPolicyT

trait SynchronousTrainerT[Action, Transition] extends TrainerT {
  var epsilon: Double

  val replayQueue: ReplayQueue[Transition]

  val policy: MovieLensPolicyT[Action]

  val rLConfig: RLConfigT
  val networkConfig: NetworkConfigT
}