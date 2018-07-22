package com.zhiwei.rl.agents.movielens

import akka.actor.{ActorRef, Props}
import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.apis.AsynchronousDQNMovieLensAgentWorkerActorT
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.policys.movielens.ItemBasedDoubleDQNMovieLensMovieLensPolicy
import com.zhiwei.types.rltypes.DQNType.Action
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx

object AsynchronousItemBasedDQNMovieLensAgentWorkerActor {
  def props(
             clusterConfig: ClusterConfigT,
             dQNNetwork: Network,
             trainerActorRef: ActorRef,
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
             dataSetMacro: MovieLensDataSetMacro
           ): Props = {
    Props(
      new AsynchronousItemBasedDQNMovieLensAgentWorkerActor(
        clusterConfig,
        dQNNetwork,
        trainerActorRef,
        rLConfig,
        recSysConfig,
        rLMacro,
        dataSetMacro
      )
    )
  }
}

class AsynchronousItemBasedDQNMovieLensAgentWorkerActor(
                                                          clusterConfig: ClusterConfigT,
                                                          dQNNetwork: Network,
                                                          override val trainerActorRef: ActorRef,
                                                          override val rLConfig: RLConfigT,
                                                          override val recSysConfig: RecSysConfigT,
                                                          override val rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
                                                          override val dataSetMacro: MovieLensDataSetMacro
                                                        )
  extends AsynchronousDQNMovieLensAgentWorkerActorT {

  val dQNPolicy:  ItemBasedDoubleDQNMovieLensMovieLensPolicy =
    new ItemBasedDoubleDQNMovieLensMovieLensPolicy(
      recSysConfig,
      dataSetMacro,
      rLConfig,
      dQNNetwork.clone,
      Network.getPseudoNetwork
  )

  def getRewardRecommendedMovieIndexes(action: Action): List[MovieIdx] = {
    List(action)
  }
}