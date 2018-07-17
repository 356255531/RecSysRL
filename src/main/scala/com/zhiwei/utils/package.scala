package com.zhiwei

import java.io.File

import com.mongodb.client.MongoCursor
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance
import org.nd4j.linalg.factory.Nd4j
import org.deeplearning4j.util.ModelSerializer


import scala.collection.mutable.ListBuffer
import scala.collection.immutable.Queue

import com.zhiwei.rl.networks.Network


package object utils {
  def convertMongoCursor2Anys[A](iterator: MongoCursor[A]): List[A] = {
    var anys: ListBuffer[A] = ListBuffer()
    while (iterator.hasNext) {
      anys += iterator.next
    }
    anys.toList
  }

  def convertIterator2AnysWithBatchSize[A](
                                               iterator: Iterator[A],
                                               batchSize: Int
                                             ): List[A] = {
    (0 until batchSize)
      .toList
      .map(
        x => {
          if (iterator.hasNext)
            Some(iterator.next)
          else None
        }
      )
      .filter(_.isDefined)
      .map(_.get)
  }


  def convertMongoCursor2AnysWithBatchSize[A](
                                               iterator: MongoCursor[A],
                                               batchSize: Int
                                             ): List[A] = {
    (0 until batchSize)
      .toList
      .map(
        x => {
          if (iterator.hasNext)
            Some(iterator.next)
          else None
        }
      )
      .filter(_.isDefined)
      .map(_.get)
  }

  def throwNotImplementedError(msg: Any, actorName: String): Unit =
    throw new NotImplementedError("Message " + msg + " not implement in Actor " + actorName)

  def computePairWiseEuclideanDistance(
                                        pointsA: INDArray,
                                        pointsB: INDArray
                                      ): INDArray = {

    Nd4j
      .getExecutioner
      .exec(
        new EuclideanDistance(
          pointsA,
          pointsB,
          true
        ),
        1
      )
  }

  def loadNetwork(
              modelPath: String,
              conf: MultiLayerConfiguration
            ): Network = {
    val file = new File(modelPath)

    if (!file.isFile) {
      println("Create new network!")
      new Network(new MultiLayerNetwork(conf))
    }
    else {
      println("Load network from file!")
      val model = ModelSerializer.restoreMultiLayerNetwork(modelPath)
      new Network(model)
    }
  }

  class FiniteQueue[A](q: Queue[A]) {
    def enqueueFinite[B >: A](elem: B, maxSize: Int): Queue[B] = {
      var ret = q.enqueue(elem)
      while (ret.size > maxSize) { ret = ret.dequeue._2 }
      ret
    }
  }
  implicit def queue2FiniteQueue[A](q: Queue[A]): FiniteQueue[A] =
    new FiniteQueue[A](q)
}
