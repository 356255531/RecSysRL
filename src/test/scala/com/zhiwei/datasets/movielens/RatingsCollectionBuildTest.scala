package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object RatingsCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro


  class RatingsCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "RatingsCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      RatingsCollectionBuilder.props(dataSetMacro),
      "MovieLensRatingsCollectionBuildActor"
    )
  }
  val movieLensDBBuildSystemName = "RatingsCollectionBuildTestActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new RatingsCollectionBuildTestActor
    ),
    "RatingsCollectionBuildTestActorRef"
  )

  test
}
