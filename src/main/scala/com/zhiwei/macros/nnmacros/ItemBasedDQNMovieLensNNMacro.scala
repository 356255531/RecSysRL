package com.zhiwei.macros.nnmacros

import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.weights.WeightInit

import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction

import com.zhiwei.configs.dqnconfigs.DQNConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro

class ItemBasedDQNMovieLensNNMacro(
                                  dQNConfig: DQNConfigT,
                                  dataSetMacro: MovieLensDataSetMacro,
                                  rLConfig: RLConfigT
                                ) extends NNMacro {
  val nIn: Int = dataSetMacro.numObservationEntry * rLConfig.N
  val nOut: Int = dataSetMacro.numReducedMovies
  val lr: Double = dQNConfig.learningRate

  val nnConfig: MultiLayerConfiguration =
    new NeuralNetConfiguration.Builder()
      .seed(dQNConfig.seed)
      .weightInit(WeightInit.XAVIER)
      .updater(new Nesterovs(lr, 0.9))
      .list()
      .layer(
        0,
        new DenseLayer
        .Builder()
          .nIn(nIn)
          .nOut(1000)
          .activation(Activation.RELU)
          .build()
      )
      .layer(
        1,
        new DenseLayer
        .Builder()
          .nIn(1000)
          .nOut(2000)
          .activation(Activation.RELU)
          .build()
      )
      .layer(
        2,
        new DenseLayer
        .Builder()
          .nIn(2000)
          .nOut(1000)
          .activation(Activation.RELU)
          .build()
      )
      .layer(
        3,
        new OutputLayer
        .Builder(LossFunction.MSE)
          .activation(Activation.IDENTITY)
          .nIn(1000)
          .nOut(nOut)
          .build()
      )
      .pretrain(false)
      .backprop(true).build()

  val folder: String = s"NIn${nIn}NOut${1}LR$lr/"

  val filePath: String = "model/MovieLensClassBasedModel/" + folder
}
