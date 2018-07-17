package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool

import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object GenomeTagsCollectionBuilder {
  def props(dataSetMacro: MovieLensDataSetMacro): Props =
    Props(
      new GenomeTagsCollectionBuilder(dataSetMacro)
    )
}

class GenomeTagsCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.genomeTagsCollectionName,
    dataSetMacro.genomeTagsCollectionAscendIndexes,
    dataSetMacro.genomeTagsCollectionDescendIndexes
) {
  val dBName: String = dataSetMacro.dBName
  val genomeTagsCollectionName: String = dataSetMacro.genomeTagsCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numProcessors)
      .props(
        DocumentsSaver.props(
          dBName,
          genomeTagsCollectionName,
          dataSetMacro
        )
      ),
    "GenomeTagsCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef = context.actorOf(
    GenomeTagsCollectionReader.props(
      dataSetMacro,
      collectionSaverActorRef
    ),
    "GenomeTagsCollectionReaderActor"
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
