package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.configs.recsysconfigs
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object MovieFeaturesCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro
  val recSysConfig = recsysconfigs.Config_1

  class MovieFeaturesCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {
    val testActorName = "MovieFeaturesCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      MovieFeaturesCollectionBuilder.props(
        dataSetMacro,
        recSysConfig
      ),
      "MovieFeaturesCollectionBuildActor"
    )
  }

  val movieLensDBBuildSystemName = "MovieFeaturesCollectionBuildTestActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new MovieFeaturesCollectionBuildTestActor
    ),
    "MovieFeaturesCollectionBuildTestActorRef"
  )

  test
}

