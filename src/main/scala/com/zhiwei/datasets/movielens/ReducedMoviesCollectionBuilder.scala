package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object ReducedMoviesCollectionBuilder {
  def props(dataSetMacro: MovieLensDataSetMacro):Props = {
    Props(
      new ReducedMoviesCollectionBuilder(dataSetMacro)
    )
  }
}

class ReducedMoviesCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.reducedMoviesCollectionName,
    dataSetMacro.reducedMoviesCollectionAscendIndexes,
    dataSetMacro.reducedMoviesCollectionDescendIndexes
){
  val dBName: String = dataSetMacro.dBName
  val reducedMoviesCollectionName: String = dataSetMacro.reducedMoviesCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()
  val numSaver: Int = numProcessors / 8

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numSaver).props(
      DocumentsSaver.props(
        dBName,
        reducedMoviesCollectionName,
        dataSetMacro
      )
    ),
    "ReducedMoviesCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef =
    context.actorOf(
      ReducedMoviesCollectionReader.props(
        dataSetMacro,
        collectionSaverActorRef
      ),
      "ReducedMoviesCollectionReaderActor"
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