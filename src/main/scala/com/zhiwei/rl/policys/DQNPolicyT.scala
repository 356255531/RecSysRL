package com.zhiwei.rl.policys

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import com.zhiwei.rl.networks.Network
import com.zhiwei.types.rltypes.RLBaseType.{State, Reward}

trait DQNPolicyT[Action] extends PolicyT{
  val qFunctionCarrier: Network

  var epsilon: Double

  def getPolicy: DQNPolicyT[Action]

  def saveQFunctionCarrier(savePath: String, iteration: Long): Unit =
    qFunctionCarrier.save(savePath, iteration: Long)

  def setEpsilon(newEpsilon: Double): Unit =
    epsilon = newEpsilon

  def initQFunctionCarrier(): Unit =
    qFunctionCarrier.init()

  def convertState2NNInput(state: State): INDArray = {
    val shape = state.shape
    state.dup().reshape(1, shape(0) * shape(1))
  }

  def getNextAction(currentState: State): Action

  def getLabels(
                 states: List[State],
                 actions: List[Action],
                 rewards: List[Reward],
                 nextStates: List[State]
               ): INDArray = {
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

    labelVector
  }

  def fit(input: INDArray, labels: INDArray): Double =
    qFunctionCarrier.fit(input, labels)

  def eval(input: INDArray): INDArray =
    qFunctionCarrier.eval(input)
}