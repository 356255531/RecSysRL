package com.zhiwei.datasets.movielens

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait CollectionBuildTestT extends App {
  abstract class AbstractCollectionBuildTestActor extends Actor with
    ActorLogging{
    val testActorName: String

    val CollectionBuilderActorRef: ActorRef

    override def preStart(): Unit =
      log.info(s"${testActorName} starts.")

    override def postStop(): Unit = {
      context.system.stop(context.parent)
      log.info(s"${testActorName} close.")
    }

    override def receive: Receive = {
      case CollectionBuildRequest =>
        CollectionBuilderActorRef ! CollectionBuildRequest
      case CollectionBuildResult(_, "Finished") => {
        sender ! PoisonPill
        context.stop(self)
      }
    }
  }

  val movieLensDBBuildSystemName: String
  val movieLensDBBuildSystem: ActorSystem
  val collectionBuildTestActorRef: ActorRef

  def test: Unit = {
    collectionBuildTestActorRef ! CollectionBuildRequest
    Await.ready(movieLensDBBuildSystem.whenTerminated, Duration.Inf)
  }
}
