
package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object MovieClassFeaturesCollectionBuilder {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             clusterConfig: ClusterConfigT
           ):Props = {
    Props(
      new MovieClassFeaturesCollectionBuilder(
        dataSetMacro,
        clusterConfig
      )
    )
  }
}

class MovieClassFeaturesCollectionBuilder(
                                          dataSetMacro: MovieLensDataSetMacro,
                                          clusterConfig: ClusterConfigT,
                                    ) extends AbstractCollectionBuilder(
  dataSetMacro.dBName,
  dataSetMacro.movieClassFeaturesCollectionName,
  dataSetMacro.movieClassFeaturesCollectionAscendIndexes,
  dataSetMacro.movieClassFeaturesCollectionDescendIndexes
){
  val dBName: String = dataSetMacro.dBName
  val movieClassFeaturesCollectionName: String = dataSetMacro.movieClassFeaturesCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()
  val numSaver: Int = numProcessors / 8
  val numQueryWorkers: Int = numProcessors - numSaver

  val collectionSaverActorRef: ActorRef =
    context.actorOf(
      BalancingPool(numSaver).props(
        DocumentsSaver.props(
          dBName,
          movieClassFeaturesCollectionName,
          dataSetMacro
        )
      ),
      "MovieClassFeaturesCollectionSaverRouterActor"
    )

  val collectionReaderActorRef: ActorRef = context.actorOf(
    MovieClassFeaturesCollectionReader.props(
      dataSetMacro,
      numQueryWorkers,
      collectionSaverActorRef,
      clusterConfig
    ),
    "MovieClassFeaturesCollectionReaderActor"
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