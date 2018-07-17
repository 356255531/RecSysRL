package com.zhiwei.datasets

import akka.actor.{Actor, ActorLogging}

object AbstractWorker {
  case class WorkRequest(request: Any)
  case class WorkResult(documents: Any)
}

abstract class AbstractWorker(
                               collectionName: String
                             ) extends Actor with ActorLogging {
  val actorName: String = collectionName + "QueryWorkerActor"

  override def preStart(): Unit =
    log.info(s"$actorName starts.")

  override def postStop(): Unit =
    log.info(s"$actorName close.")
}