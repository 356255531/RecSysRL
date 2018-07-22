package com.zhiwei.rl.agents.movielens

import akka.actor.{ActorRef, Props}

import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.apis.AsynchronousDQNMovieLensAgentWorkerActorT
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.policys.movielens.ClassBasedDoubleDQNMovieLensMovieLensPolicy
import com.zhiwei.types.rltypes.DQNType.Action
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx

object AsynchronousClassBasedDQNMovieLensAgentWorkerActor {
  def props(
             clusterConfig: ClusterConfigT,
             dQNNetwork: Network,
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
             dataSetMacro: MovieLensDataSetMacro,
             trainerActorRef: ActorRef
           ): Props = {
    Props(
      new AsynchronousClassBasedDQNMovieLensAgentWorkerActor(
        clusterConfig,
        dQNNetwork,
        rLConfig,
        recSysConfig,
        rLMacro,
        dataSetMacro,
        trainerActorRef
      )
    )
  }
}

class AsynchronousClassBasedDQNMovieLensAgentWorkerActor(
                                                         clusterConfig: ClusterConfigT,
                                                         dQNNetwork: Network,
                                                         override val rLConfig: RLConfigT,
                                                         override val recSysConfig: RecSysConfigT,
                                                         override val rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
                                                         override val dataSetMacro: MovieLensDataSetMacro,
                                                         override val trainerActorRef: ActorRef
                                                        )
  extends AsynchronousDQNMovieLensAgentWorkerActorT {

  val dQNPolicy:  ClassBasedDoubleDQNMovieLensMovieLensPolicy =
    new ClassBasedDoubleDQNMovieLensMovieLensPolicy(
      clusterConfig,
      recSysConfig,
      dataSetMacro,
      movieClassFeaturesCollection,
      rLConfig,
      dQNNetwork.clone,
      Network.getPseudoNetwork
    )

  def getRewardRecommendedMovieIndexes(action: Action): List[MovieIdx] = {
    List(action)
  }
}