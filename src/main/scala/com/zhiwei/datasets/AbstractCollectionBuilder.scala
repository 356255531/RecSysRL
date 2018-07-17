package com.zhiwei.datasets

import akka.actor.{Actor, ActorLogging, ActorRef}

import com.mongodb.MongoClient

import org.bson.Document

object AbstractCollectionBuilder {
  final case object CollectionBuildRequest
  final case class CollectionBuildResult(
                                          collectionName: String,
                                          res: String
                                        )
}

abstract class AbstractCollectionBuilder(
                                  dBName: String,
                                  collectionName: String,
                                  ascendIndexes: List[String],
                                  descendIndexes: List[String]
                                ) extends Actor with ActorLogging{
  val actorName: String = collectionName + "BuildActor"

  def initCollection(): Unit = {
    val client = new MongoClient()
    val collection =
      client
        .getDatabase(dBName)
        .getCollection(collectionName)
    collection.drop()
    client.close()
  }

  override def preStart(): Unit = {
    initCollection()
    log.info(s"$actorName starts.")
  }

  override def postStop(): Unit = {
    val client = new MongoClient()
    val collection =
      client.
        getDatabase(dBName).
        getCollection(collectionName)
    log.info("Indexing....")
    ascendIndexes.foreach(idx => collection.createIndex(new Document(idx, 1)))
    descendIndexes.foreach(idx => collection.createIndex(new Document(idx, 0)))
    client.close()
    log.info(s"$actorName close.")
  }

  val collectionSaverActorRef: ActorRef

  val collectionReaderActorRef: ActorRef
}