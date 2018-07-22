package com.zhiwei.rl.policys.movielens

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import org.bson.Document
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.policys.movielens.apis.DoubleDQNMovieLensPolicyT
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.Action
import org.nd4j.linalg.api.ndarray.INDArray

class ItemBasedDoubleDQNMovieLensMovieLensPolicy(
                                                   recSysConfig: RecSysConfigT,
                                                   dataSetMacro: MovieLensDataSetMacro,
                                                   override val rLConfig: RLConfigT,
                                                   override var onlineNetwork: Network,
                                                   override var targetNetwork: Network
                                                )
  extends DoubleDQNMovieLensPolicyT {
  // Get the number of movies
  val dBName: String = dataSetMacro.dBName
  val reducedMoviesCollectionName: String =
    dataSetMacro.reducedMoviesCollectionName
  val client: MongoClient = new MongoClient()
  val reducedMoviesCollection: MongoCollection[Document] =
    client.getDatabase(dBName).getCollection(reducedMoviesCollectionName)
  val numActions: Int = reducedMoviesCollection.count().toInt
  client.close()

  def getRecommendedMovieIndexes(
                                  action: Action,
                                  nNOutput: INDArray,
                                  isRandomAction: Boolean
                                ): List[MovieIdx] = {
    val nNOutputArray: Array[Double] = nNOutput.toDoubleVector
    nNOutputArray
      .zipWithIndex
      .sortWith(_._1 < _._1)
      .map(_._2)
      .takeRight(recSysConfig.numRecommendations)
      .toList
  }

  def getPolicy: ItemBasedDoubleDQNMovieLensMovieLensPolicy = {
    new ItemBasedDoubleDQNMovieLensMovieLensPolicy(
      recSysConfig,
      dataSetMacro,
      rLConfig,
      onlineNetwork.clone,
      targetNetwork.clone
    )
  }
}

