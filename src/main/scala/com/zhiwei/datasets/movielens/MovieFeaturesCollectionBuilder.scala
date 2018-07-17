package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.mongodb.MongoClient
import org.bson.Document
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.datasets.{AbstractCollectionBuilder, DocumentsSaver}
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object MovieFeaturesCollectionBuilder {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             recSysConfig: RecSysConfigT
           ):Props = {
    Props(
      new MovieFeaturesCollectionBuilder(
        dataSetMacro,
        recSysConfig
      )
    )
  }
}

class MovieFeaturesCollectionBuilder(
                                      dataSetMacro: MovieLensDataSetMacro,
                                      recSysConfig: RecSysConfigT
                                    )
  extends AbstractCollectionBuilder(
    dataSetMacro.dBName,
    dataSetMacro.movieFeaturesCollectionName,
    dataSetMacro.movieFeaturesCollectionAscendIndexes,
    dataSetMacro.movieFeaturesCollectionDescendIndexes
){
  val dBName: String = dataSetMacro.dBName
  val movieFeaturesCollectionName: String = dataSetMacro.movieFeaturesCollectionName

  val numProcessors: Int = Runtime.getRuntime.availableProcessors
  val numSaver: Int = numProcessors / 8
  val numQueryWorkers: Int = numProcessors - numSaver

  val collectionSaverActorRef: ActorRef =
    context.actorOf(
      BalancingPool(numSaver).props(
        DocumentsSaver.props(
          dBName,
          movieFeaturesCollectionName,
          dataSetMacro
        )
      ),
      "MovieFeaturesCollectionSaverRouterActor"
    )

  val collectionReaderActorRef: ActorRef = context.actorOf(
    MovieFeaturesCollectionReader.props(
      numQueryWorkers,
      recSysConfig,
      dataSetMacro,
      collectionSaverActorRef
    ),
    "MovieFeaturesCollectionReaderActor"
  )

  override def postStop(): Unit = {
    super.postStop()
    val client = new MongoClient()
    val movieFeaturesCollection =
      client.
        getDatabase(dBName).
        getCollection(movieFeaturesCollectionName)
    log.info("Indexing 2D Sphere....")
    movieFeaturesCollection.createIndex(new Document("featureVector", "2dsphere"))
    client.close()
  }

  def receive: PartialFunction[Any, Unit]  = {
    case CollectionBuildRequest =>
      collectionReaderActorRef ! CollectionBuildRequest
    case CollectionBuildResult(collectionName, "Finished") =>
      context.parent ! CollectionBuildResult(collectionName, "Finished")
      context.stop(self)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}