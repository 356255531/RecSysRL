package com.zhiwei.configs.networkconfigs

import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.nd4j.weightinit.WeightInit

object AsynClassBasedMovieLensDQNConfig extends NetworkConfigT {
  val seed = new scala.util.Random(System.currentTimeMillis()).nextInt
  val weightInt = WeightInit.XAVIER
  val learningRate = 0.001
  val lossFunction = LossFunction.MSE
  val preTrain = false
  val backProp = true

  val fileName: String = "AsynClassBasedMovieLensDQN.zip"
}
