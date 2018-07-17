package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object ReducedMoviesCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro


  class ReducedMoviesCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "ReducedMoviesCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      ReducedMoviesCollectionBuilder.props(dataSetMacro),
      "ReducedMoviesCollectionBuildActor"
    )
  }
  val movieLensDBBuildSystemName = "ReducedRatingsCollectionBuildTestActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new ReducedMoviesCollectionBuildTestActor
    ),
    "ReducedRatingsCollectionBuildTestActorRef"
  )

  test
}


