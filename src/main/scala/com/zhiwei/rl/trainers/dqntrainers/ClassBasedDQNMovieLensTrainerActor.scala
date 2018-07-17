package com.zhiwei.rl.trainers.dqntrainers

import akka.actor.{ActorRef, PoisonPill, Props}
import akka.routing.BalancingPool
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.AbstractAgent.{LearnRequest, LearnResult}
import com.zhiwei.rl.agents.movielens.ClassBasedDQNMovieLensAgent
import com.zhiwei.rl.memories.ReplayQueue
import com.zhiwei.rl.policys.movielens.ClassBasedDQNMovieLensPolicy
import com.zhiwei.rl.trainers.TrainerT.TrainRequest
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.types.rltypes.movielens.ClassBasedDQNMovieLensRLType.Transition
import com.zhiwei.types.rltypes.movielens.ClassBasedDQNMovieLensRLType.{Action, Reward, State, Transitions}
import com.zhiwei.rl.trainers.dqntrainers.ClassBasedDQNMovieLensTrainerActor.{ChangeEpsilonRequest, FitPolicyRequest, FitPolicySuccess, GetNextActionRequest, GetNextActionResult, ReplayQueueEnqueueRequest, ReplayQueueGetBatchRequest, ReplayQueueGetBatchResult}


object ClassBasedDQNMovieLensTrainerActor {
  final case class GetNextActionRequest(state: State)
  final case class GetNextActionResult(action: Action)
  final case class ReplayQueueEnqueueRequest(transition: Transition)
  final case class ReplayQueueGetBatchRequest(batchSize: Int)
  final case class ReplayQueueGetBatchResult(transition: Option[Transitions])
  final case class FitPolicyRequest(
                                     states: List[State],
                                     actions: List[Action],
                                     rewards: List[Reward],
                                     nextStates: List[State]
                                   )
  final case class FitPolicySuccess(newTrainLoss: Double)
  final case class ChangeEpsilonRequest(epsilon: Double)

  def props(
             rLConfig: RLConfigT,
             recSysConfig: RecSysConfigT,
             replayQueueConfig: ReplayQueueConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
             policy: ClassBasedDQNMovieLensPolicy
           ): Props = {
    Props(
      new ClassBasedDQNMovieLensTrainerActor(
        rLConfig,
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        policy
      )
    )
  }
}

class ClassBasedDQNMovieLensTrainerActor(
                                          rLConfig: RLConfigT,
                                          recSysConfig: RecSysConfigT,
                                          replayQueueConfig: ReplayQueueConfigT,
                                          dataSetMacro: MovieLensDataSetMacro,
                                          rLMacro: ClassBasedDQNMovieLensNGramRLMacro,
                                          override val policy: ClassBasedDQNMovieLensPolicy
                                    )
  extends DQNTrainerActorT[Action, Transition]  {



  val trainerName = "ClassBasedMovieLensDQNTrainer"

  var stillRunningAgent = 0

  val replayQueue: ReplayQueue[Transition] =
    new ReplayQueue[Transition](
      replayQueueConfig.queueSize,
      replayQueueConfig.minGetBatchSize
    )

  // To determine when training ends
  var numTrainEpisode = 0
  val numTrainEpisodeThreshold = 100
  def ifLearnTerminated: Boolean =
    numTrainEpisode > numTrainEpisodeThreshold

  val agentRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numAgent).props(
      ClassBasedDQNMovieLensAgent.props(
        rLConfig,
        recSysConfig,
        dataSetMacro,
        rLMacro,
        self,
      )
    )
    ,
    "MovieLensClassBasedDQNAgentRouter"
  )

  override def receive: Receive = {
    case TrainRequest =>
      agentRouterActorRef !  LearnRequest
    case GetNextActionRequest(state: State) =>
      val action = policy.getNextAction(state)
      sender ! GetNextActionResult(action)
    case ReplayQueueEnqueueRequest(transition: Transition) =>
      replayQueue.enqueueTransition(transition)
    case ReplayQueueGetBatchRequest(batchSize: Int) =>
      val transitionOption = replayQueue.getTransitionBatchOption(batchSize)
      sender ! ReplayQueueGetBatchResult(transitionOption)
    case FitPolicyRequest(
    states: List[State],
    actions: List[Action],
    rewards: List[Reward],
    nextStates: List[State]
    ) =>
      val error = policy.fit(states, actions, rewards, nextStates)
      sender ! FitPolicySuccess(error)
    case ChangeEpsilonRequest(newEpsilon: Double) =>
      policy.setEpsilon(newEpsilon)
    case LearnResult =>
      numTrainEpisode += 1
      if (ifLearnTerminated) {
        (0 until numAgent)
          .foreach(_ => agentRouterActorRef ! PoisonPill)
        context.system.stop(context.parent)
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}