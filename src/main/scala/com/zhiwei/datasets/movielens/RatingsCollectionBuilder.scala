package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object RatingsCollectionBuilder {
  type UserId = String
  type MovieId = String
  type Rating = Float
  type TimeStamp = Long

  type InsertValues= List[String]
  type InsertValuesBatch = List[InsertValues]
  type InsertElement = Map[String, String]
  type InsertElements = List[InsertElement]

  def props(dataSetMacro: MovieLensDataSetMacro): Props =
    Props(
      new RatingsCollectionBuilder(dataSetMacro)
    )
}

class RatingsCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.ratingsCollectionName,
    dataSetMacro.ratingsCollectionAscendIndexes,
    dataSetMacro.ratingsCollectionDescendIndexes
  ) {
  val dBName: String = dataSetMacro.dBName
  val ratingsCollectionName: String = dataSetMacro.ratingsCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numProcessors)
      .props(
          DocumentsSaver.props(
            dBName,
            ratingsCollectionName,
            dataSetMacro
          )
      ),
    "RatingsCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef = context.actorOf(
    RatingsCollectionReader.props(
      dataSetMacro,
      collectionSaverActorRef
    ),
    "RatingsCollectionReaderActor"
  )

  def receive: PartialFunction[Any, Unit]  = {
    case CollectionBuildRequest =>
      initCollection()
      collectionReaderActorRef ! CollectionBuildRequest
    case CollectionBuildResult(collectionName, "Finished") =>
      context.parent forward CollectionBuildResult(collectionName, "Finished")
      context.stop(self)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}