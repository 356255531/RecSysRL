package com.zhiwei.datasets

import akka.actor.{Actor, ActorLogging, Props}

import collection.JavaConverters._

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection

import org.bson.Document

import com.zhiwei.types.dbtypes.DBBaseType.MongoDBManyDocuments
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import DocumentsSaver.{CollectionBuildSaveRequest, CollectionBuildSaveResult}

object DocumentsSaver {
  final case class CollectionBuildSaveRequest(mongoManyDocuments: MongoDBManyDocuments)
  final case class CollectionBuildSaveResult(info: String)

  def props(
             dbName: String,
             collectionName: String,
             dataSetMacro: MovieLensDataSetMacro
           ): Props =
    Props(
      new DocumentsSaver(
        dbName,
        collectionName,
        dataSetMacro)
    )
}

class DocumentsSaver(
                      dbName: String,
                      collectionName: String,
                      dataSetMacro: MovieLensDataSetMacro
                    ) extends Actor with ActorLogging{

  def save(mongoDBManyDocuments: MongoDBManyDocuments): Unit = {
    if (mongoDBManyDocuments.asScala.nonEmpty)
      collection.insertMany(mongoDBManyDocuments)
  }

  val client = new MongoClient()
  val collection: MongoCollection[Document] =
    client.getDatabase(dbName).getCollection(collectionName)

  override def preStart(): Unit =
    log.info(s"${collectionName}CollectionSaverActor starts.")

  override def postStop(): Unit = {
    client.close()
    log.info(s"${collectionName}CollectionSaverActor close.")
  }

  def receive: PartialFunction[Any, Unit] = {
    case CollectionBuildSaveRequest(mongoManyDocuments: MongoDBManyDocuments) =>
      val startSaveTime = System.nanoTime
      save(mongoManyDocuments)
      if (dataSetMacro.TIME_DEBUG)
        log.info(
          s"This database update costs ${(System.nanoTime - startSaveTime)/ 1e9d} seconds.")
      sender ! CollectionBuildSaveResult("Finished")
    case msg =>
      throwNotImplementedError(msg, self.toString())
  }
}