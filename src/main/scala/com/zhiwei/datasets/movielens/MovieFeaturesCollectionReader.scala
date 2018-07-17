package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoCursor}
import org.bson.Document
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.datasets.AbstractDBDependentCollectionBuildReader
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.types.dbtypes.DBBaseType.MongoDBManyDocuments
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.convertMongoCursor2AnysWithBatchSize

object MovieFeaturesCollectionReader {
  def props(
             numQueryWorkers: Int,
             recSysConfig: RecSysConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new MovieFeaturesCollectionReader(
        numQueryWorkers,
        recSysConfig,
        dataSetMacro,
        collectionSaverActorRef
      )
    )
}
class MovieFeaturesCollectionReader(
                                     numQueryWorkers: Int,
                                     recSysConfig: RecSysConfigT,
                                     dataSetMacro: MovieLensDataSetMacro,
                                     collectionSaverActorRef: ActorRef
                           ) extends AbstractDBDependentCollectionBuildReader(
                                          dataSetMacro.movieFeaturesCollectionName
) {
  val dBName:String = dataSetMacro.dBName
  val movieFeaturesCollectionName: String = dataSetMacro.movieFeaturesCollectionName
  val enhancedMoviesCollectionName: String = dataSetMacro.enhancedMoviesCollectionName

  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  val client = new MongoClient()
  val enhancedMoviesCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(enhancedMoviesCollectionName)

  var numQueryRequestNoResponse: Int = 0

  val readIterator: MongoCursor[Document] = enhancedMoviesCollection.find.iterator

  var queryWorkDone: Boolean = !readIterator.hasNext

  val queryReadWorkerRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numQueryWorkers).props(
      MovieFeaturesCollectionQueryWorker.props(
        dataSetMacro,
        self
      )
    ),
    "MovieFeaturesQueryWorkerRouterActor"
  )

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getQueryWorkRequestOption(): Option[WorkRequest] =
  {
    val docs = convertMongoCursor2AnysWithBatchSize(readIterator, saveBatchSize)

    docs.foreach(_.remove("_id"))

    if (docs.isEmpty) None
    else Some(WorkRequest(docs))
  }

  def getQueryWorkRequests(
                                 n: Int
                               ): List[WorkRequest] = {
    val queryUpdateWorkRequests = (0 until n)
      .toList
      .map(_ => getQueryWorkRequestOption())
      .filter(_.isDefined)
      .map(_.get)

    queryUpdateWorkRequests
  }

  def receive: PartialFunction[Any, Unit] = {
    case CollectionBuildRequest =>
      if (queryWorkDone) throw new ExceptionInInitializerError("No query works!")
      val  queryWorkRequests = getQueryWorkRequests(numQueryWorkers)
      queryWorkDone = !readIterator.hasNext
      queryWorkRequests.foreach(queryReadWorkerRouterActorRef ! _)
      numQueryRequestNoResponse += queryWorkRequests.size
    case WorkResult(docsToSave: MongoDBManyDocuments) =>
      collectionSaverActorRef ! CollectionBuildSaveRequest(docsToSave)
    case CollectionBuildSaveResult("Finished" ) =>
      numQueryRequestNoResponse -= 1
      if (queryWorkDone) {
        if (0 == numQueryRequestNoResponse) {
          context.parent ! CollectionBuildResult(movieFeaturesCollectionName, "Finished")
        }
      }
      else {
        val  queryWorkRequests = getQueryWorkRequests(1)
        queryWorkDone = !readIterator.hasNext
        queryWorkRequests.foreach(queryReadWorkerRouterActorRef ! _)
        numQueryRequestNoResponse += queryWorkRequests.size
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}

