package com.zhiwei.datasets

import akka.actor.{Actor, ActorLogging}
import com.zhiwei.datasets.AbstractWorker.WorkRequest

abstract class AbstractDBDependentCollectionBuildReader(
                                                  collectionName: String
                                                ) extends Actor with ActorLogging {
  val actorName: String = collectionName + "ReaderActor"

  val readIterator: Any

  override def preStart(): Unit =
    log.info(s"$actorName starts.")

  override def postStop(): Unit = {
    log.info(s"$actorName close.")
  }

  def getQueryWorkRequests(n: Int): List[WorkRequest]
}

