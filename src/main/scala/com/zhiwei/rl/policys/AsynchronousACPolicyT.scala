package com.zhiwei.rl.policys

import org.deeplearning4j.nn.gradient.Gradient

import org.nd4j.linalg.api.ndarray.INDArray

import com.zhiwei.rl.networks.Network
import com.zhiwei.types.rltypes.RLBaseType.{State, Reward}

object AsynchronousACPolicyT extends  PolicyT{
  final case object GetACPolicyRequest

  final case object GlobalCounterIncreaseRequest

  final case class ApplyGradientRequest(
                                         policyGradient: Gradient,
                                         valueGradient: Gradient,
                                         batchSize: Int
                                       )
  final case class ApplyGradientResult(loss: Double)
}

trait AsynchronousACPolicyT[Action] extends PolicyT {
  val actorNetwork: Network

  val criticNetwork: Network

  val scope: String

  def getPolicy: AsynchronousACPolicyT[Action]

  def saveActorCritic(savePath: String, iteration: Long): Unit ={
    if (scope == "local") throw new IllegalArgumentException("Local ACPolicy can not be saved!")
    actorNetwork.save(savePath + "/Actor/", iteration: Long)
    criticNetwork.save(savePath + "/Critic/", iteration: Long)
  }

  def initActorCritic(): Unit = {
    if (!actorNetwork.ifInit) actorNetwork.init()
    if (!criticNetwork.ifInit) criticNetwork.init()
  }

  def convertState2NNInput(state: State): INDArray = {
    val shape = state.shape
    state.dup().reshape(1, shape(0) * shape(1))
  }

  def getNextAction(currentState: State): Action

  def getGradients(
                   states: List[State],
                   actions: List[Action],
                   rewards: List[Reward]
                 ): (Gradient, Gradient, Int)

  def applyGradients(
                     policyGradient: Gradient,
                     valueGradient: Gradient,
                     batchSize: Int
                   ): Unit

  def evalActor(input: INDArray): INDArray =
    actorNetwork.eval(input)

  def evalCritic(input: INDArray): INDArray =
    criticNetwork.eval(input)

  def fitCritic(inputs: INDArray, labels: INDArray): Double =
    criticNetwork.fit(inputs, labels)
}
