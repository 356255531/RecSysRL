package com.zhiwei.rl.networks

import java.io.File

import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.gradient.Gradient
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.api.ndarray.INDArray

object Network {
  val conf: MultiLayerConfiguration =
    new NeuralNetConfiguration.Builder()
      .list()
      .build()
  def getPseudoNetwork: Network = {
    new Network(new MultiLayerNetwork(conf), "local")
  }
}

class Network(neuralNetwork: MultiLayerNetwork, scope: String) {
  var ifInit: Boolean = neuralNetwork.isInitCalled
  init()

  /**
    * randomize the weights of neural network
    */
  def init(): Unit =
    if (!ifInit) neuralNetwork.init()

  /**
    * fit from input and labels
    *
    * @param input  input batch
    * @param labels target batch
    */
  def fit(input: INDArray, labels: INDArray): Double = {
    neuralNetwork.fit(input, labels)
    val prediction = neuralNetwork.output(input)
    scala.math.sqrt(labels.squaredDistance(prediction) / input.shape()(0))
  }

  /**
    * @param batch batch to evaluate
    * @return evaluation by the model of the input by all outputs
    */
  def eval(input: INDArray): INDArray =
    neuralNetwork.output(input)

  /**
    * Calculate the gradients from input and label (target) of all outputs
    *
    * @param input  input batch
    * @param labels target batch
    * @return the gradients
    */
  def getGradient(input: INDArray, labels: INDArray): Gradient = {
    neuralNetwork.setInput(input)
    neuralNetwork.setLabels(labels)
    neuralNetwork.computeGradientAndScore()
    val iterationListeners = neuralNetwork.getListeners
    if (iterationListeners != null)
      iterationListeners.forEach(_.onGradientCalculation(neuralNetwork))
    neuralNetwork.gradient
  }

  /**
    * update the params from the gradients and the batchSize
    *
    * @param gradients gradients to apply the gradient from
    * @param batchSize batchSize from which the gradient was calculated on (similar to nstep)
    */
  def applyGradient(gradients: Gradient, batchSize: Int): Unit = {
    val neuralNetworkConfiguration: MultiLayerConfiguration =
      neuralNetwork.getLayerWiseConfigurations
    val iterationCount = neuralNetworkConfiguration.getIterationCount
    val epochCount = neuralNetwork.getEpochCount
    neuralNetwork.getUpdater.update(neuralNetwork, gradients,
      iterationCount, epochCount, batchSize, LayerWorkspaceMgr.noWorkspaces)
    neuralNetwork.params.subi(gradients.gradient)
  }

  /**
    * save the neural net into a filename
    *
    * @param filePath filepath to save in
    */
  def save(fileName: String): Unit = {
    val file: File = new File(fileName)       //Where to save the network. Note: the file is in .zip format - can be opened externally
    val saveUpdater: Boolean = true           //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
    ModelSerializer.writeModel(neuralNetwork, file, saveUpdater)
  }

  override def clone: Network =
    new Network(neuralNetwork.clone(), "local")
}