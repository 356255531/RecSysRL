package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool

import org.bson.Document

import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoCursor}

import com.zhiwei.datasets.AbstractDBDependentCollectionBuildReader
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.types.dbtypes.DBBaseType.MongoDBManyDocuments
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro

import scala.collection.mutable.ListBuffer

object ReducedRatingsCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             numQueryWorkers: Int,
             collectionSaverActorRef: ActorRef
           ): Props =
    Props(
      new ReducedRatingsCollectionReader(
        dataSetMacro,
        numQueryWorkers,
        collectionSaverActorRef
      )
    )
}

class ReducedRatingsCollectionReader(
                                      dataSetMacro: MovieLensDataSetMacro,
                                      numQueryWorkers: Int,
                                      collectionSaverActorRef: ActorRef
                           ) extends AbstractDBDependentCollectionBuildReader(
                                          dataSetMacro.reducedRatingsCollectionName
) {
  val dBName: String = dataSetMacro.dBName
  val reducedRatingsCollectionName: String = dataSetMacro.reducedRatingsCollectionName
  val ratingsCollectionName: String = dataSetMacro.ratingsCollectionName
  val saveBatchSize: Int = dataSetMacro.saveBatchSize


  val client: MongoClient = new MongoClient()
  val ratingsCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(ratingsCollectionName)

  var numQueryRequestNoResponse: Int = 0

  val readIterator: MongoCursor[Document] = ratingsCollection.find.iterator

  var queryWorkDone: Boolean = !readIterator.hasNext

  val queryReadWorkerRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numQueryWorkers).props(
      ReducedRatingsCollectionQueryWorker.props(
        dataSetMacro,
        self
      )
    ),
    "ReducedRatingsCollectionQueryWorkerRouterActor"
  )

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def convertIterator2Anys[A](iterator: MongoCursor[A]): List[A] = {
    var anys = ListBuffer[A]()
    while (iterator.hasNext)
      anys += iterator.next
    anys.toList
  }

  def getQueryWorkRequestOption(): Option[WorkRequest] =
  {
    val docs = (0 until saveBatchSize)
      .toList
      .map(
        x =>
          if (readIterator.hasNext)
            Some(readIterator.next)
          else None
      )
      .filter(_ .isDefined)
      .map(_.get)

    docs.foreach(_.remove("_id"))

    if (docs.isEmpty) None
    else Some(WorkRequest(docs))
  }

  def getQueryWorkRequests(
                                 n: Int
                               ): List[WorkRequest] = {
    val queryUpdateWorkRequests =
      (0 until n)
        .map(_ => getQueryWorkRequestOption())
        .filter(_.isDefined)
        .map(_.get)
        .toList

    queryUpdateWorkRequests
  }

  def receive: PartialFunction[Any, Unit] = {
    case CollectionBuildRequest =>
      if (queryWorkDone) throw new ExceptionInInitializerError("No query works!")
      val  queryWorkRequests = getQueryWorkRequests(numQueryWorkers)
      queryWorkDone = !readIterator.hasNext
      queryWorkRequests.foreach(queryReadWorkerRouterActorRef ! _)
      numQueryRequestNoResponse += queryWorkRequests.size
    case WorkResult(updateDocs: MongoDBManyDocuments) =>
      collectionSaverActorRef ! CollectionBuildSaveRequest(updateDocs)
    case CollectionBuildSaveResult("Finished" ) =>
      numQueryRequestNoResponse -= 1
      if (queryWorkDone) {
        if (0 == numQueryRequestNoResponse) {
          context.parent ! CollectionBuildResult(reducedRatingsCollectionName, "Finished")
        }
      }
      else {
        val queryWorkRequests = getQueryWorkRequests(1)
        queryWorkDone = !readIterator.hasNext
        queryWorkRequests.foreach(queryReadWorkerRouterActorRef ! _)
        numQueryRequestNoResponse += queryWorkRequests.size
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}

