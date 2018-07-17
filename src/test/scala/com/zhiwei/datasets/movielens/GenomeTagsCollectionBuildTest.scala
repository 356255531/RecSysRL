package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object GenomeTagsCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro


  class GenomeTagsCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "GenomeTagsCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      GenomeTagsCollectionBuilder.props(dataSetMacro),
      "GenomeTagsCollectionBuildActor"
    )
  }
  val movieLensDBBuildSystemName = "GenomeTagsCollectionBuildActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new GenomeTagsCollectionBuildTestActor
    ),
    "GenomeTagsCollectionBuildTestActorRef"
  )

  test
}

