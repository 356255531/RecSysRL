package com.zhiwei.rl.agents.movielens

import akka.actor.{ActorRef, Props}
import com.mongodb.client.MongoCursor
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.apis.SynchronousDQNMovieLensAgentWorkerActorT
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.Action
import com.zhiwei.utils.convertMongoCursor2Anys
import org.bson.Document

object ClassBasedDQNMovieLensAgentWorkerActor {
  def props(
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
             trainerActorRef: ActorRef
           ): Props = {
    Props(
      new ClassBasedDQNMovieLensAgentWorkerActor(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        trainerActorRef
      )
    )
  }
}

class ClassBasedDQNMovieLensAgentWorkerActor(
                                              override val rLConfig: RLConfigT,
                                              override val recSysConfig: RecSysConfigT,
                                              override val dataSetMacro: MovieLensDataSetMacro,
                                              override val rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
                                              override val trainerActorRef: ActorRef
                                        )
  extends SynchronousDQNMovieLensAgentWorkerActorT {

  val random = scala.util.Random

  def getRewardRecommendedMovieIndexes(action: Action): List[MovieIdx] = {
    val movieClassDocsIterator: MongoCursor[Int] =
      movieClassFeaturesCollection
        .find(new Document("class", action))
        .map(_.get("movieIdx", classOf[java.lang.Integer]).toInt)
        .iterator()
    random
      .shuffle(convertMongoCursor2Anys(movieClassDocsIterator))
      .take(recSysConfig.numRecommendations)
  }
}