package com.zhiwei.rl.trainers.dqntrainers

import akka.actor.{Actor, ActorLogging}
import com.zhiwei.rl.memories.ReplayQueue
import com.zhiwei.rl.trainers.{SynchronousT, TrainerT}

trait DQNTrainerActorT[Action, Transition] extends Actor
  with ActorLogging with TrainerT[Action] with SynchronousT {
  val replayQueue: ReplayQueue[Transition]

  override def preStart(): Unit = {
    log.info(s"${trainerName}Actor starts.")
  }

  override def postStop(): Unit = {
    log.info(s"${trainerName}Actor ends.")
  }
}
