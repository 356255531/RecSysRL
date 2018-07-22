package com.zhiwei.rl.policys.movielens

import org.bson.Document
import org.nd4j.linalg.api.ndarray.INDArray
import com.mongodb.client.{MongoCollection, MongoCursor}
import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.policys.movielens.apis.DoubleDQNMovieLensPolicyT
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.Action
import com.zhiwei.utils.convertMongoCursor2Anys

class ClassBasedDoubleDQNMovieLensMovieLensPolicy(
                                    clusterConfig: ClusterConfigT,
                                    recSysConfig: RecSysConfigT,
                                    dataSetMacro: MovieLensDataSetMacro,
                                    movieClassFeaturesCollection: MongoCollection[Document],
                                    override val rLConfig: RLConfigT,
                                    override var onlineNetwork: Network,
                                    override var targetNetwork: Network
                                  )
  extends DoubleDQNMovieLensPolicyT {

  val numActions: Int = clusterConfig.numCluster

  def getRecommendedMovieIndexes(
                                  action: Action,
                                  nNOutput: INDArray,
                                  isRandomAction: Boolean
                                ): List[MovieIdx] = {
    val movieClass: Int =
      if (isRandomAction)
        nNOutput.argMax(1).getInt(0, 0)
      else action

    val recommendedMovieIdxMongoCursor: MongoCursor[Int] =
      movieClassFeaturesCollection
        .find(new Document("class", movieClass))
        .map(_.get("movieIdx", classOf[java.lang.Integer]).toInt)
        .iterator()
    val recommendedMovieIndexes: List[MovieIdx] =
      convertMongoCursor2Anys(recommendedMovieIdxMongoCursor)
    val random = scala.util.Random
    random.shuffle(recommendedMovieIndexes).take(recSysConfig.numRecommendations)
  }

  def getPolicy: ClassBasedDoubleDQNMovieLensMovieLensPolicy = {

    new ClassBasedDoubleDQNMovieLensMovieLensPolicy(
      clusterConfig,
      recSysConfig,
      dataSetMacro,
      movieClassFeaturesCollection,
      rLConfig,
      onlineNetwork.clone,
      targetNetwork.clone
    )
  }
}

