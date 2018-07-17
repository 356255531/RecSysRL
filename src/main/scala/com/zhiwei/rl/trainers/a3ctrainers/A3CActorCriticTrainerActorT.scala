package com.zhiwei.rl.trainers.a3ctrainers

import akka.actor.{Actor, ActorLogging}
import com.zhiwei.rl.trainers.{AsynchronousT, TrainerT}

trait A3CActorCriticTrainerActorT[Action] extends Actor
  with ActorLogging with TrainerT[Action] with AsynchronousT {

  override def preStart(): Unit = {
    log.info(s"${trainerName}Actor starts.")
  }

  override def postStop(): Unit = {
    log.info(s"${trainerName}Actor ends.")
  }
}

