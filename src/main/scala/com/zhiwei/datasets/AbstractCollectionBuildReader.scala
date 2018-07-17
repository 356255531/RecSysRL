package com.zhiwei.datasets

import akka.actor.{Actor, ActorLogging}

import com.zhiwei.datasets.DocumentsSaver.CollectionBuildSaveRequest

abstract class AbstractCollectionBuildReader(
                                 collectionName: String
                               ) extends Actor with ActorLogging {

  val actorName: String = collectionName + "ReaderActor"

  val readIterator: Any

  override def preStart(): Unit =
    log.info(s"$actorName starts.")

  override def postStop(): Unit =
    log.info(s"$actorName close.")

  def getSaveRequests(n: Int): List[CollectionBuildSaveRequest]
}

