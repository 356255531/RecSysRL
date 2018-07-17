package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}
import com.mongodb.client.MongoCollection
import com.mongodb.MongoClient
import com.zhiwei.datasets.AbstractWorker
import org.bson.Document

import scala.collection.JavaConverters._
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import com.zhiwei.datasets.AbstractWorker.{WorkRequest, WorkResult}
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.utils.throwNotImplementedError

object MovieFeaturesCollectionQueryWorker {
  def props(
             dataSetMacro: MovieLensDataSetMacro,
             documentReaderActorRef: ActorRef
           ): Props =
    Props(
      new MovieFeaturesCollectionQueryWorker(
        dataSetMacro,
        documentReaderActorRef
      )
    )
}

class MovieFeaturesCollectionQueryWorker(
                                          dataSetMacro: MovieLensDataSetMacro,
                                          documentReaderActorRef: ActorRef
                                )
  extends AbstractWorker(dataSetMacro.movieFeaturesCollectionName) {

  var fine: Boolean = true

  val dBName: String = dataSetMacro.dBName
  val genomeTagsCollectionName: String = dataSetMacro.genomeTagsCollectionName

  val client: MongoClient = new MongoClient()
  val genomeTagsCollection: MongoCollection[Document] =
    client
      .getDatabase(dBName)
      .getCollection(genomeTagsCollectionName)

  val numGenomeTags: Int = genomeTagsCollection.count().toInt

  override def postStop(): Unit = {
    client.close()
    super.postStop()
  }

  def convertYear2ContentFeature(year: Int): INDArray = {
    val yearArray = Array.fill[Double](6)(0)
    year match {
      case a if a > 0 && a < 1980 =>
        yearArray.update(0, 1)
      case b if b < 1990 =>
        yearArray.update(1, 1)
      case c if c < 2000 =>
        yearArray.update(2, 1)
      case d if d < 2010 =>
        yearArray.update(3, 1)
      case e if e >= 2010 =>
        yearArray.update(4, 1)
      case f if f == 0 =>
        yearArray.update(5, 1)
    }
    Nd4j.create(yearArray)
  }

  def convertNumRating2ContentFeature(numRating: Long): INDArray = {
    val numRatingsArray = Array.fill[Double](5)(0)
    numRating match {
      case a if a < 10 =>
        numRatingsArray.update(0, 1)
      case b if b < 100 =>
        numRatingsArray.update(1, 1)
      case c if c < 1000 =>
        numRatingsArray.update(2, 1)
      case d if d < 10000 =>
        numRatingsArray.update(3, 1)
      case _ =>
        numRatingsArray.update(4, 1)
    }
    Nd4j.create(numRatingsArray)
  }

  def convertAverageRating2ContentFeature(averageRating: Double): INDArray = {
    Nd4j.create(Array[Double](averageRating / 5))
  }

  def convertGenomeScoreDocs2ContentFeature(genomeTagTupleDoc: Document): INDArray = {
    val featureArray = Array.fill[Double](numGenomeTags)(0.0)

    genomeTagTupleDoc
      .keySet()
      .forEach(
        tagId => {
          val relevance =
            genomeTagTupleDoc
              .get(tagId, classOf[java.lang.Double])
              .toDouble
              .formatted("%.3f")
              .toDouble
          featureArray.update(tagId.toInt - 1, relevance)
        }
      )

    Nd4j.create(featureArray)
  }

  def receive: PartialFunction[Any, Unit] = {
    case WorkRequest(movieDocs: List[Document])=>
      val years = movieDocs.map(_.get("year", classOf[java.lang.Integer]).toInt)
      val yearVectors = years.map(convertYear2ContentFeature)

      val numRatings = movieDocs.map(_.get("numRating", classOf[java.lang.Long]).toLong)
      val numRatingVectors = numRatings.map(convertNumRating2ContentFeature)

      val averageRatings = movieDocs.map(_.get("averageRating", classOf[java.lang.Double]).toDouble)
      val averageRatingVectors = averageRatings.map(convertAverageRating2ContentFeature)

      val genomeScoresBatch = movieDocs.map(_.get("genomeScores", classOf[Document]))
      val genomeScoresVectors = genomeScoresBatch.map(convertGenomeScoreDocs2ContentFeature)

      val featureVectors =
        movieDocs
          .indices
          .map(
            idx =>
              Nd4j.concat(
                1,
                averageRatingVectors(idx),
                genomeScoresVectors(idx),
                numRatingVectors(idx),
                yearVectors(idx)
              )
          )
        .toList
      val docsToSave =
        movieDocs
          .indices
          .map(
            idx => {
              val doc = new Document(movieDocs(idx))
              val geoJson = new Document("type", "Point")
              doc.append("featureVector", featureVectors(idx))
              doc.remove("genomeScores")
              doc
            }
          )
          .toList
      documentReaderActorRef ! WorkResult(docsToSave.asJava)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}