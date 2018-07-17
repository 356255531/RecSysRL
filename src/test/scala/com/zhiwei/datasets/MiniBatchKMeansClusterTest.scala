package com.zhiwei.datasets.clusters

import com.mongodb.MongoClient
import com.zhiwei.configs.clusterconfigs
import com.zhiwei.datasets.MiniBatchKMeansCluster
import com.zhiwei.macros.datasetmacros.movielens.MovieLensLargeDataSetMacro

object MiniBatchKMeansClusterTest extends App {
  val dataSetMacro = MovieLensLargeDataSetMacro
  val clusterConfig = clusterconfigs.Config_1

  val collection = new MongoClient()
    .getDatabase(dataSetMacro.dBName)
    .getCollection(dataSetMacro.movieFeaturesCollectionName)

  val centroids = MiniBatchKMeansCluster.getCentroidMatrix(
    collection,
    clusterConfig,
  )

  println(centroids.toString())
}