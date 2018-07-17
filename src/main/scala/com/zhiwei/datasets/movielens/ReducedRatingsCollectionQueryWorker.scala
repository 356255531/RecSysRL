package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import com.mongodb.client.{MongoCollection, MongoCursor}
import com.mongodb.MongoClient
import org.bson.Document

import scala.collection.JavaConverters._

import com.zhiwei.datasets.AbstractWorker
import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.utils.convertMongoCursor2Anys

object ReducedRatingsCollectionQueryWorker {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             documentReaderActorRef: ActorRef
           ): Props =
    Props(
      new ReducedRatingsCollectionQueryWorker(
        dataSetMacro,
        documentReaderActorRef
      )
    )
}

class ReducedRatingsCollectionQueryWorker(
                                           dataSetMacro: MovieLensDataSetMacro,
                                           documentReaderActorRef: ActorRef
                                       )
  extends AbstractWorker(dataSetMacro.reducedRatingsCollectionName) {
  val dBName: String = dataSetMacro.dBName
  val genomeScoresCollectionName: String = dataSetMacro.genomeScoresCollectionName

  val client = new MongoClient()
  val genomeScoresCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(genomeScoresCollectionName)

  val genomeScoreMovieIdsIterator: MongoCursor[java.lang.Long] =
    genomeScoresCollection.distinct("movieId", classOf[java.lang.Long]).iterator
  val genomeScoreMovieIds: List[Long] = convertMongoCursor2Anys(genomeScoreMovieIdsIterator).map(_.toLong)

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def getDocsToSave(ratingDocs: Documents): Documents = {
    val reducedRatingDocs =
      ratingDocs
        .filter(
          ratingDoc =>
            genomeScoreMovieIds
              .contains(ratingDoc.get("movieId", classOf[java.lang.Long]).toLong)
        )

    reducedRatingDocs
  }

  def receive: PartialFunction[Any, Unit] = {
    case WorkRequest(ratingDocs: Documents)=>
      val docToSave = getDocsToSave(ratingDocs)
      documentReaderActorRef ! WorkResult(docToSave.asJava)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}