package com.zhiwei.rl.trainers.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.ClassBasedDQNMovieLensAgentWorkerActor
import com.zhiwei.rl.memories.ReplayQueue
import com.zhiwei.rl.policys.movielens.ClassBasedDoubleDQNMovieLensMovieLensPolicy
import com.zhiwei.rl.trainers.movielens.apis.SynchronousMovieLensDQNTrainerActorT
import com.zhiwei.types.rltypes.DQNType.{Action, Transition}

object ClassBasedDQNMovieLensTrainerActor {
  def props(
             recSysConfig: RecSysConfigT,
             replayQueueConfig: ReplayQueueConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
             rLConfig: RLConfigT,
             networkConfig: NetworkConfigT,
             policy: ClassBasedDoubleDQNMovieLensMovieLensPolicy
           ): Props = {
    Props(
      new ClassBasedDQNMovieLensTrainerActor(
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

class ClassBasedDQNMovieLensTrainerActor(
                                          recSysConfig: RecSysConfigT,
                                          replayQueueConfig: ReplayQueueConfigT,
                                          dataSetMacro: MovieLensDataSetMacro,
                                          rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
                                          override val rLConfig: RLConfigT,
                                          override val networkConfig: NetworkConfigT,
                                          override val policy: ClassBasedDoubleDQNMovieLensMovieLensPolicy
                                    )
  extends SynchronousMovieLensDQNTrainerActorT[Action] {

  var epsilon: Double = rLConfig.epsilon

  val trainerName = "ClassBasedMovieLensDQNTrainer"

  val replayQueue: ReplayQueue[Transition] =
    new ReplayQueue[Transition](
      replayQueueConfig.queueSize,
      replayQueueConfig.minGetBatchSize
    )

  val numWorker: Int = 1
  val workerRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numWorker).props(
      ClassBasedDQNMovieLensAgentWorkerActor.props(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        self
      )
    )
    ,
    "MovieLensClassBasedDQNAgentRouter"
  )
}