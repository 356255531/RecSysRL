package com.zhiwei.rl.policys.movielens.apis

import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.rl.networks.Network
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.DQNType.{Action, Reward, State}
import com.zhiwei.utils.convertState2NNInput
import org.deeplearning4j.nn.gradient.Gradient
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import scala.util.Random

trait DoubleDQNMovieLensPolicyT extends MovieLensPolicyT[Action] {
  val random: Random = new scala.util.Random()

  val rLConfig: RLConfigT

  var onlineNetwork: Network
  var targetNetwork: Network

  var epsilon: Double = rLConfig.epsilon

  val numActions: Int

  def saveDQN(fileName: String): Unit =
    onlineNetwork.save(fileName)

  def saveNetwork(filename: String): Unit =
    saveDQN(filename)

  def updateOnlineNetwork(newValueNetwork: Network): Unit =
    onlineNetwork = newValueNetwork

  def updateTargetNetwork(): Unit =
    targetNetwork = onlineNetwork.clone

  def updateNetwork(network_1: Option[Network], network_2: Option[Network]): Unit = {
    if (network_1.isEmpty && network_2.isEmpty)
      updateTargetNetwork()
    else if (network_1.isDefined && network_2.isEmpty) updateOnlineNetwork(network_1.get)
    else throw new IllegalArgumentException("DQN update network failed!")
  }

  def randomActionSelection: Action = {
    random.nextInt(numActions)
  }

  def greedyActionSelection(state: State): (Action, INDArray) = {
    val inputVector = convertState2NNInput(state)
    val outputVector = onlineNetwork.eval(inputVector)
    (outputVector.argMax(1).getInt(0), outputVector)
  }

//  def LinUCBActionSelection(state: State): Int = {
//    val stateInput = convertState2NNInput(state)
//    val armFeatureVectors =
//      movieFeatureVectors
//        .map(
//          armFeatureVector =>
//            Nd4j.concat(1, stateInput, armFeatureVector)
//        )
//    ucbActionSelector.getNextAction(armFeatureVectors)
//  }
//
//  override def setEpsilon(newEpsilon: Reward): Unit = {
//    ucbActionSelector.setEpsilon(newEpsilon)
//  }
//
//  def updateLinUCBActionSelector(
//                    armIdx: Int,
//                    armFeatureVector: INDArray,
//                    qFunction: Double
//                  ): Unit =
//    ucbActionSelector.updateLinUCB(armIdx, armFeatureVector, qFunction)

  def softMaxActionSelection(state: State): (Action, INDArray) = {
    val inputVector: INDArray = convertState2NNInput(state)
    val outputVector: INDArray = onlineNetwork.eval(inputVector)
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

    (idx, outputVector)
  }

  def epsilonActionSelection: Action =
    randomActionSelection

  def getNextAction(state: State): (Action, INDArray, Boolean) = {
    if (scala.util.Random.nextDouble() < epsilon) {
      val action: Action = epsilonActionSelection
      val nNInput: INDArray = convertState2NNInput(state)
      val nNOutput: INDArray = onlineNetwork.eval(nNInput)
      (action, nNOutput, true)
    }
    else {
      val (action, nNOutput) = softMaxActionSelection(state)
      (action, nNOutput, false)
    }
  }

  def getNextActionAndRecommendedMovieIndexes(state: State): (Action, List[MovieIdx]) = {
    val (action, nNOutput, isRandom) = getNextAction(state)
    val recommendedMovieIndexes: List[MovieIdx] = getRecommendedMovieIndexes(action, nNOutput, isRandom)
    (action, recommendedMovieIndexes)
  }

  def getLabels(
                 states: List[State],
                 actions: List[Action],
                 rewards: List[Reward],
                 nextStates: List[State]
               ): (INDArray, INDArray) = {
    val stateInputMatrix =
      Nd4j.concat(0, states.map(convertState2NNInput).toArray: _*)
    val stateOutputMatrix = targetNetwork.eval(stateInputMatrix)

    val nextStateInputMatrix =
      Nd4j.concat(0, nextStates.map(convertState2NNInput).toArray: _*)
    val nextStateOutputMatrix = targetNetwork.eval(nextStateInputMatrix)
    val maxNextStateActionValue = nextStateOutputMatrix.max(1)

    val rewardVector = Nd4j.create(rewards.toArray).transpose()

    val targetVector = rewardVector.add(maxNextStateActionValue.mul(rLConfig.gamma))

    actions
      .indices
      .foreach(
        idx =>
          stateOutputMatrix.putScalar(
            idx,
            actions(idx),
            targetVector.getDouble(idx, 0)
          )
      )

    (stateInputMatrix, stateOutputMatrix)
  }

  def getGradient(input: INDArray, output: INDArray): Gradient =
    onlineNetwork.getGradient(input, output)

  def getGradient(
                   reverseStateHistory: List[State],
                   reverseActionHistory: List[Action],
                   reverseRewardHistory: List[Reward]
                 ): (Gradient, Int) = {
    val batchSize: Int = reverseStateHistory.size

    val reversedStateInputMatrix: INDArray =
      Nd4j.concat(
        0,
        reverseStateHistory.map(convertState2NNInput):_*
      )

    val reversedStateValueMatrix: INDArray = targetNetwork.eval(reversedStateInputMatrix)
    val reversedR: Array[Double] =
      reverseActionHistory
        .zipWithIndex
        .map(
          tuple =>
            reversedStateValueMatrix.getDouble(tuple._2, tuple._1)
        )
        .toArray
        .zip(reverseRewardHistory)
        .map(
          tuple =>
            tuple._1 * rLConfig.gamma + tuple._2
        )
    reverseActionHistory
      .zipWithIndex
      .foreach(
        tuple =>
          reversedStateValueMatrix.putScalar(tuple._2, tuple._1, reversedR(tuple._2))
      )

    val valueNetworkGradient = getGradient(reversedStateInputMatrix, reversedStateValueMatrix)

    (valueNetworkGradient, batchSize)
  }

  def fit(stateInputMatrix: INDArray, labelMatrix: INDArray): Double =
    onlineNetwork.fit(stateInputMatrix, labelMatrix)

  def fit(
           states: List[State],
           actions: List[Action],
           rewards: List[Reward],
           nextStates: List[State]
         ): Double = {
    val (stateInputMatrix, labelMatrix) =
      getLabels(
        states,
        actions,
        rewards,
        nextStates
      )

    fit(stateInputMatrix, labelMatrix)
  }

  def setEpsilon(newEpsilon: Double): Unit =
    epsilon = newEpsilon
}