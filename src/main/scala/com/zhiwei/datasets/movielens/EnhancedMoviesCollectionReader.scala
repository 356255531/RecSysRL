package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool

import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoCursor}

import org.bson.Document

import com.zhiwei.datasets.AbstractDBDependentCollectionBuildReader
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.types.dbtypes.DBBaseType.MongoDBManyDocuments
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.convertMongoCursor2AnysWithBatchSize

object EnhancedMoviesCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             numQueryWorkers: Int,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new EnhancedMoviesCollectionReader(
        dataSetMacro,
        numQueryWorkers,
        collectionSaverActorRef
      )
    )
}

class EnhancedMoviesCollectionReader(
                                      dataSetMacro: MovieLensDataSetMacro,
                                      numQueryWorkers: Int,
                                      collectionSaverActorRef: ActorRef
                           ) extends AbstractDBDependentCollectionBuildReader(
                                          dataSetMacro.enhancedMoviesCollectionName
) {
  val dBName:String = dataSetMacro.dBName
  val enhancedMoviesCollectionName: String = dataSetMacro.enhancedMoviesCollectionName
  val reducedMoviesCollectionName: String = dataSetMacro.reducedMoviesCollectionName
  val saveBatchSize: Int = dataSetMacro.saveBatchSize


  val client: MongoClient = new MongoClient()
  val reducedMoviesCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(reducedMoviesCollectionName)

  var numQueryRequestNoResponse = 0

  val readIterator: MongoCursor[Document] = reducedMoviesCollection.find.iterator

  var queryWorkDone: Boolean = !readIterator.hasNext

  val queryReadWorkerRouter: ActorRef = context.actorOf(
    BalancingPool(numQueryWorkers).props(
      EnhancedMoviesCollectionQueryWorker.props(
        dataSetMacro,
        self
      )
    ),
    "EnhancedMoviesCollectionQueryWorkerRouterActor"
  )

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getQueryWorkRequestOption(): Option[WorkRequest] =
  {
    val docs =
      convertMongoCursor2AnysWithBatchSize(readIterator, saveBatchSize)

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
      queryWorkRequests.foreach(queryReadWorkerRouter ! _)
      numQueryRequestNoResponse += queryWorkRequests.size
    case WorkResult(updateDocs: MongoDBManyDocuments) =>
      collectionSaverActorRef ! CollectionBuildSaveRequest(updateDocs)
    case CollectionBuildSaveResult("Finished" ) =>
      numQueryRequestNoResponse -= 1
      if (queryWorkDone) {
        if (0 == numQueryRequestNoResponse) {
          context.parent ! CollectionBuildResult(enhancedMoviesCollectionName, "Finished")
        }
      }
      else {
        val  queryWorkRequests = getQueryWorkRequests(1)
        queryWorkDone = !readIterator.hasNext
        queryWorkRequests.foreach(queryReadWorkerRouter ! _)
        numQueryRequestNoResponse += queryWorkRequests.size
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}

