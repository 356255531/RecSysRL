package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object MoviesCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro

  class MoviesCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "MoviesCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      MoviesCollectionBuilder.props(dataSetMacro),
      "MovieLensRatingsCollectionBuildActor"
    )
  }

  val movieLensDBBuildSystemName = "RatingsCollectionBuildTestActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new MoviesCollectionBuildTestActor
    ),
    "MoviesCollectionBuildTestActorRef"
  )

  test
}
