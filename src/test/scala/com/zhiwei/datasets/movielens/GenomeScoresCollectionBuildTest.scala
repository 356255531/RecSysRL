package com.zhiwei.datasets.movielens

import akka.actor.{ActorSystem, Props}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object GenomeScoresCollectionBuildTest extends App with CollectionBuildTestT {

  val dataSetMacro = MovieLensLargeDataSetMacro


  class GenomeScoresCollectionBuildTestActor
    extends AbstractCollectionBuildTestActor {

    val testActorName = "GenomeScoresCollectionBuildTestActor"

    val CollectionBuilderActorRef = context.actorOf(
      GenomeScoresCollectionBuilder.props(dataSetMacro),
      "GenomeScoresCollectionBuildActor"
    )
  }
  val movieLensDBBuildSystemName = "GenomeScoresCollectionBuildActorSystem"
  val movieLensDBBuildSystem = ActorSystem(movieLensDBBuildSystemName)
  val collectionBuildTestActorRef = movieLensDBBuildSystem.actorOf(
    Props(
      new GenomeScoresCollectionBuildTestActor
    ),
    "GenomeScoresCollectionBuildTestActorRef"
  )

  test
}


