package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.configs.clusterconfigs
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object MovieClassFeaturesCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro
  val clusterConfig = clusterconfigs.Config_1

  class MovieClassFeaturesCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {
    val testActorName = "MovieClassFeaturesCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      MovieClassFeaturesCollectionBuilder.props(
        dataSetMacro,
        clusterConfig
      ),
      "MovieFeaturesCollectionBuildActor"
    )
  }

  val movieLensDBBuildSystemName = "MovieClassFeaturesCollectionBuildTestActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new MovieClassFeaturesCollectionBuildTestActor
    ),
    "MovieFeaturesCollectionBuildTestActorRef"
  )

  test
}

