package com.zhiwei.datasets

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates

import scala.collection.JavaConverters._

import org.bson.Document

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance
import org.nd4j.linalg.api.ops.impl.indexaccum.IAMin
import org.nd4j.linalg.factory.Nd4j

import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.utils.{convertMongoCursor2AnysWithBatchSize, computePairWiseEuclideanDistance}

object MiniBatchKMeansCluster {
  def getRandomDocument(
                    movieFeaturesCollection: MongoCollection[Document],
                    batchSize: Int
                    ): List[Document] = {
    val docIterator =
      movieFeaturesCollection.aggregate(
          java.util.Arrays.asList(Aggregates.sample(batchSize))
      ).iterator()

    if (!docIterator.hasNext)
      throw new NoSuchElementException("No documents for KMeans cluster found!")

    convertMongoCursor2AnysWithBatchSize(docIterator, batchSize)
  }

  def getClusters(newPointsCentroidsDistMatrix: INDArray): Array[Int] = {
    Nd4j
      .getExecutioner
      .exec(
        new IAMin(newPointsCentroidsDistMatrix),
        1
      )
      .toIntVector
  }

  def updateCentroid(
                      centroid: INDArray,
                      centroidCounter: Double,
                      point: INDArray
                     ): INDArray = {
    centroid
      .mul(1 - 1 / centroidCounter)
      .add(
        point.mul(1 / centroidCounter)
      )
  }

  def updateCentroids(
                     centroids: Array[INDArray],
                     perCentroidCounter: Array[Double],
                     newPoints: List[INDArray]
                     ): (Array[INDArray], Array[Double]) = {


    val newPointMatrix =
      Nd4j.concat(0, newPoints:_*)

    val newPointsCentroidsDistMatrix =
      computePairWiseEuclideanDistance(
        newPointMatrix,
        Nd4j.concat(0, centroids:_*)
      )

    val newPointClusters = getClusters(newPointsCentroidsDistMatrix)

    // Update count per centroid
    newPointClusters
        .foreach(
          cluster =>
            perCentroidCounter
              .update(
                cluster,
                perCentroidCounter(cluster) + 1
              )
        )

    newPoints
      .zip(newPointClusters)
      .foreach {
        tuple => {
          val newCentroid =
            updateCentroid(
              centroids(tuple._2),
              perCentroidCounter(tuple._2),
              tuple._1
          )
          centroids.update(
            tuple._2,
            newCentroid
        )
      }
    }

    (centroids, perCentroidCounter)
  }

  def getCentroidMatrix(
                 movieFeaturesCollection: MongoCollection[Document],
                 clusterConfig: ClusterConfigT
                 ): INDArray= {
    val initCentroidDocs =
      getRandomDocument(
        movieFeaturesCollection,
        clusterConfig.numCluster
    )

    var perCentroidCounter =
      Array.fill[Double](clusterConfig.numCluster)(1)

    var centroids =
      initCentroidDocs
        .map(
            _
              .get(
                "featureVector",
                classOf[java.util.ArrayList[Double]]
              )
            .asScala
            .toArray
        )
      .map(Nd4j.create)
      .toArray

    1 to clusterConfig.numIteration foreach {
      iter => {
        val randomDocs =
          getRandomDocument(
            movieFeaturesCollection,
            clusterConfig.batchSize
          )
        val newPoints: List[INDArray] =
          randomDocs
            .map(
              _.get(
                "featureVector",
                classOf[java.util.ArrayList[Double]]
              )
                .asScala
                .toArray
            )
            .map(Nd4j.create)
        val (newCentroids, newPerCentroidCounter) =
          updateCentroids(
            centroids,
            perCentroidCounter,
            newPoints
          )
        centroids = newCentroids
        perCentroidCounter = newPerCentroidCounter
        print("Mini-batch KMeans work is proceeding in ")
        println(
          f"${iter * 1.00 / clusterConfig.numIteration * 100}%.2f" + "%...")
      }
    }
    Nd4j.concat(0, centroids:_*)
  }
}
