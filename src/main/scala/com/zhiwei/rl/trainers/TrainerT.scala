package com.zhiwei.rl.trainers

import akka.actor.ActorRef
import com.zhiwei.rl.policys.PolicyT

object TrainerT {
  final case object TrainRequest
}

trait TrainerT[Action] {
  val trainerName: String

  val numAgent: Int
  val agentRouterActorRef: ActorRef

  val policy: PolicyT

  def ifLearnTerminated: Boolean
}
