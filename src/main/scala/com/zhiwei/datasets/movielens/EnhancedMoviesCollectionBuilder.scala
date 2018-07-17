package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool

import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object EnhancedMoviesCollectionBuilder {
  def props(dataSetMacro: MovieLensDataSetMacro):Props = {
    Props(
      new EnhancedMoviesCollectionBuilder(dataSetMacro)
    )
  }
}

class EnhancedMoviesCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.enhancedMoviesCollectionName,
    dataSetMacro.enhancedMoviesCollectionAscendIndexes,
    dataSetMacro.enhancedMoviesCollectionDescendIndexes
){
  val dBName: String = dataSetMacro.dBName
  val enhancedMoviesCollectionName: String = dataSetMacro.enhancedMoviesCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()
  val numUpdater: Int = numProcessors / 8
  val numQueryWorkers: Int = numProcessors - numUpdater

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numUpdater).props(
      DocumentsSaver.props(
        dBName,
        enhancedMoviesCollectionName,
        dataSetMacro
      )
    ),
    "EnhancedMoviesCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef =
    context.actorOf(
      EnhancedMoviesCollectionReader.props(
        dataSetMacro,
        numQueryWorkers,
        collectionSaverActorRef
      ),
      "EnhancedMoviesCollectionReaderActor"
    )

  def receive: PartialFunction[Any, Unit]  = {
    case CollectionBuildRequest =>
      collectionReaderActorRef ! CollectionBuildRequest
    case CollectionBuildResult(collectionName, "Finished") =>
      context.parent ! CollectionBuildResult(collectionName, "Finished")
      context.stop(self)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}