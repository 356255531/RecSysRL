package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoCursor}
import org.bson.Document
import org.nd4j.linalg.api.ndarray.INDArray
import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.datasets.{AbstractDBDependentCollectionBuildReader, MiniBatchKMeansCluster}
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.types.dbtypes.DBBaseType.MongoDBManyDocuments
import com.zhiwei.datasets.AbstractCollectionBuilder.{CollectionBuildRequest, CollectionBuildResult}
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.datasets.DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.convertMongoCursor2AnysWithBatchSize


object MovieClassFeaturesCollectionReader {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             numQueryWorkers: Int,
             collectionSaverActorRef: ActorRef,
             clusterConfig: ClusterConfigT
           ): Props =
    Props(
      new MovieClassFeaturesCollectionReader(
        dataSetMacro,
        numQueryWorkers,
        collectionSaverActorRef,
        clusterConfig
      )
    )
}

class MovieClassFeaturesCollectionReader(
                                         dataSetMacro: MovieLensDataSetMacro,
                                         numQueryWorkers: Int,
                                         collectionSaverActorRef: ActorRef,
                                         clusterConfig: ClusterConfigT
                                   )
  extends AbstractDBDependentCollectionBuildReader(dataSetMacro.movieFeaturesCollectionName) {
  val dBName:String = dataSetMacro.dBName
  val movieClassFeatureCollectionName: String = dataSetMacro.movieClassFeaturesCollectionName
  val movieFeaturesCollectionName: String = dataSetMacro.movieFeaturesCollectionName
  val centroidsCollectionName: String = dataSetMacro.centroidsCollectionName
  val saveBatchSize: Int = dataSetMacro.saveBatchSize

  var numQueryRequestNoResponse = 0

  val client = new MongoClient()
  val movieFeaturesCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(movieFeaturesCollectionName)
  val centroidsCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(centroidsCollectionName)

  val readIterator: MongoCursor[Document] =
    movieFeaturesCollection
            .find()
            .iterator()

  var workDone: Boolean = !readIterator.hasNext

  // Compute centroids and save to database
  val centroidMatrix: INDArray = MiniBatchKMeansCluster.getCentroidMatrix(
    movieFeaturesCollection,
    clusterConfig
  )
  centroidsCollection.drop()
  (0 until centroidMatrix.shape()(0))
    .foreach(
      idx => {
        val centroidDoc: Document = new Document("class", idx)
        centroidDoc.append("centroid", centroidMatrix.getRow(idx))
        centroidsCollection.insertOne(centroidDoc)
      }
    )
  dataSetMacro
    .centroidsCollectionAscendIndexes
      .foreach(
        idx =>
          centroidsCollection.createIndex(new Document(idx, 1))
      )
  dataSetMacro
    .centroidsCollectionDescendIndexes
    .foreach(
      idx =>
        centroidsCollection.createIndex(new Document(idx, 0))
    )


  val queryReadWorkerRouter: ActorRef = context.actorOf(
    BalancingPool(numQueryWorkers).props(
      MovieClassFeaturesCollectionQueryWorker.props(
        movieClassFeatureCollectionName,
        centroidMatrix,
        self
      )
    ),
    "MovieClassFeaturesCollectionQueryWorkerRouterActor"
  )

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getQueryWorkRequestOption: Option[WorkRequest] = {
    val workDocs = convertMongoCursor2AnysWithBatchSize(readIterator, saveBatchSize)

    workDocs.foreach(_.remove("_id"))

    if (workDocs.isEmpty)
      None
    else Some(WorkRequest(workDocs))
  }

  def getQueryWorkRequests(
                            n: Int
                          ): List[WorkRequest] = {
    (0 until n)
      .toList
      .map(
        x =>
          getQueryWorkRequestOption
      )
      .filter(_.isDefined)
      .map(_.get)
  }

  def receive: PartialFunction[Any, Unit] = {
    case CollectionBuildRequest =>
      if (workDone) throw new ExceptionInInitializerError("No query works!")
      val queryWorkRequests = getQueryWorkRequests(numQueryWorkers)
      workDone = !readIterator.hasNext
      queryWorkRequests.foreach(queryReadWorkerRouter ! _)
      numQueryRequestNoResponse += queryWorkRequests.size
    case WorkResult(updateDocs: MongoDBManyDocuments) =>
      collectionSaverActorRef ! CollectionBuildSaveRequest(updateDocs)
    case CollectionBuildSaveResult("Finished" ) =>
      numQueryRequestNoResponse -= 1
      if (workDone) {
        if (0 == numQueryRequestNoResponse) {
          context.parent ! CollectionBuildResult(movieClassFeatureCollectionName, "Finished")
        }
      }
      else {
        val queryWorkRequests = getQueryWorkRequests(1)
        workDone = !readIterator.hasNext
        queryWorkRequests.foreach(queryReadWorkerRouter ! _)
        numQueryRequestNoResponse += queryWorkRequests.size
      }
    case msg => throwNotImplementedError(msg, self.toString())
  }
}

