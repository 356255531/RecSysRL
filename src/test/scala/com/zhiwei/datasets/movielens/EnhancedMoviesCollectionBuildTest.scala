package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object EnhancedMoviesCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro


  class EnforcedMoviesCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "EnforcedMoviesCollectionBuildTestActor"

    val CollectionBuilderActorRef =
      context.actorOf(
        EnhancedMoviesCollectionBuilder.props(dataSetMacro),
      "EnhancedMoviesCollectionBuildActor"
    )
  }
  val movieLensDBBuildSystemName = "EnhancedMoviesCollectionBuildTestActor"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new EnforcedMoviesCollectionBuildTestActor
    ),
    "EnhancedMoviesCollectionBuildTestActorRef"
  )

  test
}


