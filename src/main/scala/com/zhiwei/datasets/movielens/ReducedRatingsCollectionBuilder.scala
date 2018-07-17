package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object ReducedRatingsCollectionBuilder {
  def props(dataSetMacro: MovieLensDataSetMacro):Props = {
    Props(
      new ReducedRatingsCollectionBuilder(dataSetMacro)
    )
  }
}

class ReducedRatingsCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.reducedRatingsCollectionName,
    dataSetMacro.reducedRatingsCollectionAscendIndexes,
    dataSetMacro.reducedRatingsCollectionDescendIndexes
){
  val dBName: String = dataSetMacro.dBName
  val reducedRatingsCollectionName: String = dataSetMacro.reducedRatingsCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()
  val numSaver: Int = numProcessors / 8
  val numQueryWorkers: Int = numProcessors - numSaver

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numSaver).props(
      DocumentsSaver.props(
        dBName,
        reducedRatingsCollectionName,
        dataSetMacro
      )
    ),
    "ReducedRatingsCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef =
    context.actorOf(
      ReducedRatingsCollectionReader.props(
        dataSetMacro,
        numQueryWorkers,
        collectionSaverActorRef
      ),
      "ReducedRatingsCollectionReaderActor"
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