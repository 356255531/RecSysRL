package com.zhiwei.rl.networks

import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.gradient.Gradient
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr
import org.deeplearning4j.util.ModelSerializer

import org.nd4j.linalg.api.ndarray.INDArray

class Network(neuralNetwork: MultiLayerNetwork) {
  var ifInit: Boolean = false

  /**
    * randomize the weights of neural network
    */
  def init(): Unit =
    neuralNetwork.init()

  /**
    * fit from input and labels
    *
    * @param input  input batch
    * @param labels target batch
    */
  def fit(input: INDArray, labels: INDArray): Double = {
    neuralNetwork.fit(input, labels)
    val prediction = neuralNetwork.output(input)
    labels.squaredDistance(prediction)
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
    neuralNetwork.params.sub(gradients.gradient)
    val iterationListeners = neuralNetwork.getListeners
    if (iterationListeners != null && iterationListeners.size > 0)
      iterationListeners
        .forEach(
          _.iterationDone(
            neuralNetwork,
            iterationCount,
            epochCount)
        )
    neuralNetworkConfiguration.setIterationCount(iterationCount + 1)
  }

  /**
    * save the neural net into a filename
    *
    * @param filePath filepath to save in
    */
  def save(filePath: String, iteration: Long): Unit = {
    ModelSerializer.writeModel(
      neuralNetwork,
      filePath,
      true
    )
  }

  override def clone: Network =
    new Network(neuralNetwork.clone())
}