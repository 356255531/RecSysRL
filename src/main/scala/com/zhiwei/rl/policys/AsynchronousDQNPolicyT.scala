package com.zhiwei.rl.policys

import org.deeplearning4j.nn.gradient.Gradient
import org.nd4j.linalg.api.ndarray.INDArray

object AsynchronousDQNPolicyT extends  PolicyT{
  final case object GetAsynDQNPolicyRequest

  final case object GlobalCounterIncreaseRequest

  final case class ApplyGradientRequest(
                                         gradient: Gradient,
                                         batchSize: Int
                                       )
  final case class ApplyGradientResult(loss: Double)
}

trait AsynchronousDQNPolicyT[Action] extends DQNPolicyT[Action] {
  def getGradient(
                 input: INDArray,
                 label: INDArray
                 ): Gradient

  def applyGradient(gradient: Gradient): Unit
}