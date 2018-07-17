package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object MoviesCollectionBuilder {
  type UserId = String
  type MovieId = String
  type Rating = Float
  type TimeStamp = Long

  type Tag = String
  type Tags = List[Tag]

  type InsertValues= List[String]
  type InsertValuesBatch = List[InsertValues]
  type InsertElement = Map[String, String]
  type InsertElements = List[InsertElement]

  def props(dataSetMacro: MovieLensDataSetMacro): Props =
    Props(
      new MoviesCollectionBuilder(dataSetMacro)
    )
}

class MoviesCollectionBuilder(dataSetMacro: MovieLensDataSetMacro)
  extends AbstractCollectionBuilder(
                dataSetMacro.dBName,
                dataSetMacro.moviesCollectionName,
                dataSetMacro.moviesCollectionAscendIndexes,
                dataSetMacro.moviesCollectionDescendIndexes
){
  val dBName: String = dataSetMacro.dBName
  val moviesCollectionName: String = dataSetMacro.moviesCollectionName
  val moviesFilePath: String = dataSetMacro.moviesFilePath

  val numProcessors: Int = Runtime.getRuntime.availableProcessors()

  val collectionSaverActorRef: ActorRef = context.actorOf(
    BalancingPool(numProcessors)
      .props(
        DocumentsSaver.props(
          dBName,
          moviesCollectionName,
          dataSetMacro
        )
    ),
    "MoviesCollectionSaverRouterActor"
  )

  val collectionReaderActorRef: ActorRef = context.actorOf(
    MoviesCollectionReader.props(
      dataSetMacro,
      collectionSaverActorRef
    ),
    "MoviesCollectionReaderActor"
  )

  def receive: PartialFunction[Any, Unit]  = {
    case CollectionBuildRequest => collectionReaderActorRef ! CollectionBuildRequest
    case CollectionBuildResult(collectionName, "Finished") =>
      context.parent ! CollectionBuildResult(collectionName, "Finished")
      context.stop(self)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}