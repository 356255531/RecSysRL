package com.zhiwei.rl.trainers.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.ItemBasedDQNMovieLensAgentWorkerActor
import com.zhiwei.rl.memories.ReplayQueue
import com.zhiwei.rl.policys.movielens.ItemBasedDoubleDQNMovieLensMovieLensPolicy
import com.zhiwei.rl.trainers.movielens.apis.SynchronousMovieLensDQNTrainerActorT
import com.zhiwei.types.rltypes.DQNType.{Action, Transition}


object ItemBasedDQNMovieLensTrainerActor {
  def props(
             recSysConfig: RecSysConfigT,
             replayQueueConfig: ReplayQueueConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
             rLConfig: RLConfigT,
             networkConfig: NetworkConfigT,
             policy: ItemBasedDoubleDQNMovieLensMovieLensPolicy
           ): Props = {
    Props(
      new ItemBasedDQNMovieLensTrainerActor(
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        rLConfig,
        networkConfig,
        policy
      )
    )
  }
}

class ItemBasedDQNMovieLensTrainerActor(
                                         recSysConfig: RecSysConfigT,
                                         replayQueueConfig: ReplayQueueConfigT,
                                         dataSetMacro: MovieLensDataSetMacro,
                                         rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
                                         override val rLConfig: RLConfigT,
                                         override val networkConfig: NetworkConfigT,
                                         override val policy: ItemBasedDoubleDQNMovieLensMovieLensPolicy
                                    )
  extends SynchronousMovieLensDQNTrainerActorT[Action] {

  var epsilon: Double = rLConfig.epsilon

  val trainerName = "ItemBasedDQNMovieLensTrainer"

  val replayQueue: ReplayQueue[Transition] =
      new ReplayQueue[Transition](
        replayQueueConfig.queueSize,
        replayQueueConfig.minGetBatchSize
      )

  val numWorker: Int = 1
  val workerRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numWorker).props(
      ItemBasedDQNMovieLensAgentWorkerActor.props(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        self
      )
    )
    ,
    "ItemBasedDQNMovieLensAgentRouter"
  )
}