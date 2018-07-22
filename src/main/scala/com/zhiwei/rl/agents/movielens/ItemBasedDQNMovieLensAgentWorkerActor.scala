package com.zhiwei.rl.agents.movielens

import akka.actor.{ActorRef, Props}
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.apis.SynchronousDQNMovieLensAgentWorkerActorT
import com.zhiwei.types.rltypes.DQNType.Action
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx

object ItemBasedDQNMovieLensAgentWorkerActor {
  def props(
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
             trainerActorRef: ActorRef
           ): Props = {
    Props(
      new ItemBasedDQNMovieLensAgentWorkerActor(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        trainerActorRef
      )
    )
  }
}

class ItemBasedDQNMovieLensAgentWorkerActor(
                                             override val rLConfig: RLConfigT,
                                             override val recSysConfig: RecSysConfigT,
                                             override val dataSetMacro: MovieLensDataSetMacro,
                                             override val rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
                                             override val trainerActorRef: ActorRef
                                           )
  extends SynchronousDQNMovieLensAgentWorkerActorT {

  def getRewardRecommendedMovieIndexes(action: Action): List[MovieIdx] =
    List(action)
}