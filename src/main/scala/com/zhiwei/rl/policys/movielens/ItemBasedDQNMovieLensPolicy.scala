package com.zhiwei.rl.policys.movielens

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import org.bson.Document
import org.nd4j.linalg.factory.Nd4j
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.policys.DQNPolicyT
import com.zhiwei.types.rltypes.movielens.ItemBasedDQNMovieLensRLType.{Action, Reward, State}
import org.nd4j.linalg.api.ndarray.INDArray

class ItemBasedDQNMovieLensPolicy(
                                   rLConfig: RLConfigT,
                                   recSysConfig: RecSysConfigT,
                                   dataSetMacro: MovieLensDataSetMacro,

                      override val qFunctionCarrier: Network
                    )
  extends DQNPolicyT[Action] {

  var epsilon: Double = rLConfig.epsilon

  val dBName: String = dataSetMacro.dBName
  val movieFeaturesCollectionName: String =
    dataSetMacro.movieFeaturesCollectionName
  val client: MongoClient = new MongoClient()
  val movieFeaturesCollection: MongoCollection[Document] =
    client.getDatabase(dBName).getCollection(movieFeaturesCollectionName)
  val numMovies: Int = movieFeaturesCollection.count().toInt
  client.close()

  qFunctionCarrier.init()

  def randomActionSelection: Action =
    new scala.util.Random(System.currentTimeMillis).nextInt(numMovies)

  def epsilonActionSelection: Action =
    randomActionSelection

  def greedyActionSelection(state: State): Action = {
    val inputVector = convertState2NNInput(state)
    val outputVector = eval(inputVector)
    outputVector.argMax(0).getInt(0, 0)
  }

  def softMaxActionSelection(state: State): Action = {
    val inputVector: INDArray = convertState2NNInput(state)
    val outputVector: INDArray = eval(inputVector)
    val outputArray: Array[Double] = outputVector.toDoubleVector
    val sum: Double = outputArray.foldLeft(0.0)(_ + _)
    val normalizedOutputArray: Array[Double] = outputArray.map(_ / sum)

    val randomDouble: Double = scala.util.Random.nextDouble()

    var acc = 0.0
    var idx = 0
    while (randomDouble > acc + normalizedOutputArray(idx)) {
      acc += normalizedOutputArray(idx)
      idx += 1
    }
    idx
  }

  def getNextAction(state: State): Action = {
    if (scala.util.Random.nextDouble() < epsilon)
      epsilonActionSelection
    else softMaxActionSelection(state)
  }

  def getRecommendedMovieIds(state: State): List[Int] = {
    val inputVector: INDArray = convertState2NNInput(state)
    val outputArray: Array[Double] = eval(inputVector).toDoubleVector
    val recommendedMovieIds: List[Int] =
      outputArray.toList.zipWithIndex.sortWith(_._1 < _._1).map(_._2)
    recommendedMovieIds.take(recSysConfig.numRecommendations)
  }

  def fit(
           states: List[State],
           actions: List[Action],
           rewards: List[Reward],
           nextStates: List[State]
         ): Double = {
    val stateInputMatrix =
      Nd4j.concat(
        0,
        states.map(convertState2NNInput).toArray: _*
      )
    val stateOutputMatrix = eval(stateInputMatrix)
    val labelMatrix = stateOutputMatrix
    val selectedStateActionArray =
      (0 until stateOutputMatrix.shape()(0))
        .map(
          line =>
            stateOutputMatrix.getDouble(line, actions(line))
        )
        .toArray
    val selectedStateActionVector =
      Nd4j.create(selectedStateActionArray).transpose()

    val nextStateInputMatrix =
      Nd4j.concat(
        0,
        nextStates.map(convertState2NNInput).toArray: _*
      )
    val nextStateOutputMatrix = eval(nextStateInputMatrix)
    val maxNextStateActionValue = nextStateOutputMatrix.max(1)

    val rewardVector = Nd4j.create(rewards.toArray).transpose()

    val targetVector = rewardVector.add(maxNextStateActionValue.mul(rLConfig.gamma))
    val diff = targetVector.sub(selectedStateActionVector)
    val labelVector = selectedStateActionVector.add(diff.mul(rLConfig.learningRate))

    (0 until stateOutputMatrix.shape()(0))
      .foreach(
        line =>
          labelMatrix.putScalar(
            line,
            actions(line),
            labelVector.getDouble(line, 0)
          )
      )

    fit(stateInputMatrix, labelMatrix)
  }

  def getPolicy: ItemBasedDQNMovieLensPolicy =
    new ItemBasedDQNMovieLensPolicy(
      rLConfig,
      recSysConfig,
      dataSetMacro,
      qFunctionCarrier.clone()
    )
}

