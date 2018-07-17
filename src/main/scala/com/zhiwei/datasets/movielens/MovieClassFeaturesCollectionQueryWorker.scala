package com.zhiwei.datasets.movielens

import akka.actor.{ActorRef, Props}

import org.bson.Document

import scala.collection.JavaConverters._

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance
import org.nd4j.linalg.api.ops.impl.indexaccum.IAMin
import org.nd4j.linalg.factory.Nd4j

import com.zhiwei.datasets.AbstractWorker
import com.zhiwei.utils.throwNotImplementedError
import com.zhiwei.datasets.AbstractWorker._


object MovieClassFeaturesCollectionQueryWorker {
  def props(
             collectionName: String,
             centroidMatrix: INDArray,
             documentReaderActorRef: ActorRef
           ): Props =
    Props(
      new MovieClassFeaturesCollectionQueryWorker(
        collectionName,
        centroidMatrix,
        documentReaderActorRef
      )
    )
}


class MovieClassFeaturesCollectionQueryWorker(
                                  collectionName: String,
                                  centroidMatrix: INDArray,
                                  documentReaderActorRef: ActorRef
                                ) extends AbstractWorker(
  collectionName
) {

  def computeEuclideanDistance(
                                point: INDArray,
                                points: INDArray
                              ): INDArray = {
    Nd4j
      .getExecutioner
      .exec(
        new EuclideanDistance(
          point,
          points,
          true
        ),
        1
      )
  }

  def getClasses(doc: Document): Int = {
    val featureVector =
      Nd4j.create(
        doc
          .get(
            "featureVector",
            classOf[java.util.ArrayList[Double]]
          )
          .asScala
          .toArray
    )

    val newPointDistVector =
      computeEuclideanDistance(
        featureVector,
        centroidMatrix
      )


    Nd4j
      .getExecutioner
      .exec(
        new IAMin(newPointDistVector),
        1
      )
      .getInt(0)
  }

  def receive: PartialFunction[Any, Unit] = {
    case WorkRequest(requests: List[Document])=>
      val classes = requests.map(getClasses)
      val results =
        requests
          .zip(classes)
          .map(tuple => tuple._1.append("class", tuple._2))
          .asJava
      documentReaderActorRef ! WorkResult(results)
    case msg => throwNotImplementedError(msg, self.toString())
  }
}
