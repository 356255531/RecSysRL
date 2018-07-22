package com.zhiwei

import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction

object EZTest extends App {
  val nnConfig: MultiLayerConfiguration =
    new NeuralNetConfiguration.Builder()
      .seed(12345)
      .weightInit(WeightInit.XAVIER)
      .updater(new Nesterovs(0.001, 0.9))
      .list()
      .layer(
        0,
        new DenseLayer
        .Builder()
          .nIn(400000)
          .nOut(2000)
          .activation(Activation.RELU)
          .build()
      )
      .layer(
        1,
        new DenseLayer
        .Builder()
          .nIn(2000)
          .nOut(5000)
          .activation(Activation.RELU)
          .build()
      )
      .layer(
        2,
        new OutputLayer
        .Builder(LossFunction.MSE)
          .activation(Activation.IDENTITY)
          .nIn(5000)
          .nOut(200)
          .build()
      )
      .pretrain(false)
      .backprop(true)
      .build()

  val nn = new MultiLayerNetwork(nnConfig)
  nn.init()

  println(nn.output(Nd4j.rand(10, 400000)))
}