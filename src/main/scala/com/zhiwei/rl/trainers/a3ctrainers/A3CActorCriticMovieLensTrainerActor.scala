package com.zhiwei.rl.trainers.a3ctrainers

import akka.actor.{ActorRef, PoisonPill, Props}
import akka.routing.BalancingPool
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ContinuousMovieLensRLMacro
import com.zhiwei.rl.agents.movielens.A3CMovieLensAgent
import com.zhiwei.rl.agents.AbstractAgent.{LearnRequest, LearnResult}
import com.zhiwei.rl.policys.movielens.ActorMovieLensPolicy
import com.zhiwei.rl.trainers.TrainerT.TrainRequest
import com.zhiwei.types.rltypes.movielens.ContinuousActionMovieLensRLType.Action
import com.zhiwei.utils.throwNotImplementedError

object A3CActorCriticMovieLensTrainerActor {
  def props(
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ContinuousMovieLensRLMacro,
             policy: ActorMovieLensPolicy
           ): Props = {
    Props(
      new A3CActorCriticMovieLensTrainerActor(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        policy
      )
    )
  }
}

class A3CActorCriticMovieLensTrainerActor(
                                           rLConfig: RLConfigT,
                                           recSysConfig: RecSysConfigT,
                                           dataSetMacro: MovieLensDataSetMacro,
                                           rLMacro: ContinuousMovieLensRLMacro,
                                           override val policy: ActorMovieLensPolicy
                                       )
  extends A3CActorCriticTrainerActorT[Action]  {

  val trainerName = "A3CActorCriticMovieLensTrainer"

  var stillRunningAgent = 0

  // To determine when training ends
  var numTrainEpisode = 0
  val numTrainEpisodeThreshold = 100
  def ifLearnTerminated: Boolean =
    numTrainEpisode > numTrainEpisodeThreshold

  val agentRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numAgent).props(
      A3CMovieLensAgent.props(
        rLConfig,
        dataSetMacro,
        self,
        recSysConfig,
        rLMacro
      )
    )
    ,
    "A3CActorCriticMovieLensAgentRouter"
  )

  override def receive: Receive = {
    case TrainRequest =>
      agentRouterActorRef !  LearnRequest
    case LearnResult =>
      numTrainEpisode += 1
      if (ifLearnTerminated) {
        agentRouterActorRef ! PoisonPill
        if (0 == stillRunningAgent)
          context.system.stop(context.parent)
      }
      else agentRouterActorRef !  LearnRequest
    case msg => throwNotImplementedError(msg, self.toString())
  }
}