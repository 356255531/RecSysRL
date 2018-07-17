package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object ReducedRatingsCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro


  class ReducedRatingsCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "ReducedRatingsCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      ReducedRatingsCollectionBuilder.props(dataSetMacro),
      "ReducedRatingsCollectionBuildActor"
    )
  }
  val movieLensDBBuildSystemName = "ReducedRatingsCollectionBuildTestActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new ReducedRatingsCollectionBuildTestActor
    ),
    "ReducedRatingsCollectionBuildTestActorRef"
  )

  test
}

