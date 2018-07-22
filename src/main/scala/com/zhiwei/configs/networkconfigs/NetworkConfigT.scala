package com.zhiwei.configs.networkconfigs

import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.nd4j.weightinit.WeightInit

trait NetworkConfigT {
  val seed: Int
  val weightInt:  WeightInit
  val learningRate: Double
  val lossFunction: LossFunction
  val preTrain: Boolean
  val backProp: Boolean

  val fileName: String
}
