package com.zhiwei.macros.nnmacros

import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.weights.WeightInit

import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction

import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro

class ItemBasedDQNMovieLensNNMacro(
                                    dQNConfig: NetworkConfigT,
                                    dataSetMacro: MovieLensDataSetMacro,
                                    rLConfig: RLConfigT
                                ) extends NNMacro {
  val nIn: Int = dataSetMacro.numObservationEntry * rLConfig.N
  val nOut: Int = dataSetMacro.numReducedMovies
  val lr: Double = dQNConfig.learningRate

  val nnConfig: MultiLayerConfiguration =
    new NeuralNetConfiguration.Builder()
      .seed(dQNConfig.seed)
      .weightInit(WeightInit.NORMAL)
      .updater(new Nesterovs(lr, 0.9))
      .list()
      .layer(
        0,
        new DenseLayer
        .Builder()
          .nIn(nIn)
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
          .nOut(nOut)
          .build()
      )
      .pretrain(false)
      .backprop(true)
      .build()
}
