package com.zhiwei.configs.dqnconfigs

import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.nd4j.weightinit.WeightInit

object Config_1 extends DQNConfigT {
  val seed = new scala.util.Random(System.currentTimeMillis()).nextInt
  val weightInt = WeightInit.XAVIER
  val learningRate = 0.001
  val lossFunction = LossFunction.MSE
  val preTrain = false
  val backProp = true
}
