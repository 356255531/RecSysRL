package com.zhiwei.rl.policys.movielens

import scala.collection.JavaConverters._
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.api.rng.distribution.impl.NormalDistribution
import org.nd4j.linalg.indexing.NDArrayIndex
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.policys.AsynchronousACPolicyT
import com.zhiwei.types.rltypes.movielens.ContinuousActionMovieLensRLType.Action
import com.zhiwei.types.dbtypes.DBBaseType.{Documents, Query}
import com.zhiwei.utils.convertMongoCursor2Anys
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.types.rltypes.RLBaseType.{Reward, State}
import com.zhiwei.utils.computePairWiseEuclideanDistance
import org.deeplearning4j.nn.gradient.Gradient

object ActorMovieLensPolicy {
  final case class GetACPolicyResult(acPolicy: AsynchronousACPolicyT[Action])
}

class ActorMovieLensPolicy(
                            actions: List[Action],
                            dataSetMacro: MovieLensDataSetMacro,
                            rLConfig: RLConfigT,
                            recSysConfig: RecSysConfigT,
                            movieFeaturesCollection: MongoCollection[Document],
                            override val scope: String,
                            override val actorNetwork: Network,
                            var movieFeaturesMatrixPlaceHolder: INDArray,
                            var reversedTDErrorVectorPlaceHolder: INDArray,
                            override val criticNetwork: Network
                          )
  extends AsynchronousACPolicyT[Action] {

  val numMovies: Int = movieFeaturesCollection.count().toInt
  val movieDocs: Documents =
    convertMongoCursor2Anys(
      movieFeaturesCollection
        .find()
        .sort(new Document("movieIdx", 1))
        .iterator()
    )
  val movieFeatureMatrix: INDArray =
    Nd4j.concat(
      0,
      movieDocs
        .map(
          _
            .get(
              "featureVector",
              classOf[java.util.ArrayList[Double]])
            .asScala
            .toArray
        )
        .map(Nd4j.create):_*
    )

  initActorCritic()

  def getPolicy: ActorMovieLensPolicy = {
    if (scope == "local")
      throw new IllegalArgumentException("Local ACPolicy can not be copied!")
    new ActorMovieLensPolicy(
      actions,
      dataSetMacro,
      rLConfig,
      recSysConfig,
      movieFeaturesCollection,
      scope,
      actorNetwork,
      movieFeaturesMatrixPlaceHolder,
      reversedTDErrorVectorPlaceHolder,
      criticNetwork
    )
  }

  def getNextAction(state: State): Action = {
    val stateInputVector: INDArray = convertState2NNInput(state)
    val output: INDArray =
      evalActor(stateInputVector)
        .reshape(
          2 * recSysConfig.numRecommendations,
          dataSetMacro.numMovieContentFeatures
        )
    val meanArray: Array[Double] =
      output
        .get(
          NDArrayIndex.interval(0, recSysConfig.numRecommendations),
          NDArrayIndex.all()
        )
      .toDoubleVector
    val stdArray: Array[Double] =
      output
        .get(
          NDArrayIndex.interval(
            recSysConfig.numRecommendations,
            2 * recSysConfig.numRecommendations
          ),
          NDArrayIndex.all()
        )
        .toDoubleVector

    val normDist = new NormalDistribution
    val stdNormDistSampleArray =
      (0 until recSysConfig.numRecommendations * dataSetMacro.numMovieContentFeatures)
        .toArray
        .map(_ => normDist.sample)
    val distArray =
      stdNormDistSampleArray
        .indices
        .toArray
        .map(
          idx =>
            stdNormDistSampleArray(idx) * stdArray(idx) + meanArray(idx)
        )
    val recommendedMovieFeatureMatrix =
      Nd4j
        .create(distArray)
        .reshape(
          recSysConfig.numRecommendations,
          dataSetMacro.numMovieContentFeatures
        )
    val distMatrix = computePairWiseEuclideanDistance(recommendedMovieFeatureMatrix, movieFeatureMatrix)
    val movieIndexes = distMatrix.argMax(1).toDoubleVector.toList
    val movieIndexQueries: List[Query] = movieIndexes.map(new Document("movieIdx", _))
    val movieDocIterators: List[Document] =
      movieIndexQueries
        .map(movieFeaturesCollection.find(_).iterator())
        .map(convertMongoCursor2Anys)
        .foldLeft(List[Document]())(_ ::: _)
    movieDocIterators
  }

  def getGradients(
                   states: List[State],
                   actions: List[Action],
                   rewards: List[Reward]
                 ): (Gradient, Gradient, Int) = {
    if (scope == "global") throw new IllegalArgumentException("Global ACPolicy is not allowed to get gradient!")

    val batchSize: Int = states.size

    val reversedStateInputMatrix: INDArray =
      Nd4j.concat(
        0,
        states.reverse.map(convertState2NNInput):_*
      )

    val reversedStateValueVector: INDArray = criticNetwork.eval(reversedStateInputMatrix).transpose()
    val reversedRewardVector: INDArray = Nd4j.create(rewards.toArray.reverse).transpose()
    val reversedTargetVector: INDArray =
      reversedStateValueVector
        .get(
          NDArrayIndex.interval(1,reversedStateValueVector.shape()(0)),
          NDArrayIndex.all()
        )
        .mul(rLConfig.gamma)
        .add(reversedRewardVector)
    val reversedTDErrorVector: INDArray =
      reversedTargetVector.sub(
        reversedStateValueVector
          .get(
            NDArrayIndex.interval(1,reversedStateValueVector.shape()(0)),
            NDArrayIndex.all()
        )
      )

    val pseudoLabelVector: INDArray =
      Nd4j.zeros(
        batchSize,
        recSysConfig.numRecommendations * dataSetMacro.numMovieContentFeatures
      )
    movieFeaturesMatrixPlaceHolder =
      Nd4j.concat(
        0,
        actions.map(
          movieDocs =>
            Nd4j.concat(
              1,
              movieDocs
                .map(
                  movieDoc =>
                    Nd4j.create(
                      movieDoc
                        .get("featureVector", classOf[java.util.ArrayList[Double]])
                        .asScala
                        .toArray
                  )
                ):_*
            )
        ):_*
      )
    reversedTDErrorVectorPlaceHolder = reversedTDErrorVector
    val policyGradient: Gradient =
      actorNetwork
        .getGradient(
          reversedStateInputMatrix.get(
            NDArrayIndex.interval(1, reversedStateInputMatrix.shape()(0)),
            NDArrayIndex.all()
          ),
          pseudoLabelVector
        )

    val valueGradient: Gradient =
      criticNetwork.getGradient(
        reversedStateInputMatrix.get(
          NDArrayIndex.interval(1, reversedStateInputMatrix.shape()(0)),
          NDArrayIndex.all()
        ),
        reversedTargetVector
      )

    (policyGradient, valueGradient, batchSize)
  }

  def applyGradients(
                     policyGradient: Gradient,
                     valueGradient: Gradient,
                     batchSize: Int
                   ): Unit = {
    actorNetwork.applyGradient(policyGradient, batchSize)
    criticNetwork.applyGradient(valueGradient, batchSize)
  }
}


