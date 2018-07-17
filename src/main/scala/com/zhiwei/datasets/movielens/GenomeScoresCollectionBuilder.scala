package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool

import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object GenomeScoresCollectionBuilder {
  def props(dataSetMacro: MovieLensDataSetMacro): Props =
    Props(
      new GenomeScoresCollectionBuilder(dataSetMacro)
    )
}

class GenomeScoresCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.genomeScoresCollectionName,
    dataSetMacro.genomeScoresCollectionAscendIndexes,
    dataSetMacro.genomeScoresCollectionDescendIndexes
) {
  val dBName: String = dataSetMacro.dBName
  val genomeScoresCollectionName: String = dataSetMacro.genomeScoresCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numProcessors)
      .props(
        DocumentsSaver.props(
          dBName,
          genomeScoresCollectionName,
          dataSetMacro
        )
      ),
    "GenomeScoresCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef = context.actorOf(
    GenomeScoresCollectionReader.props(
      dataSetMacro,
      collectionSaverActorRef
    ),
    "GenomeScoresCollectionReaderActor"
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
