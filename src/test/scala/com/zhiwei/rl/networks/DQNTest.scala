//package com.zhiwei.rl.networks
//
//import org.deeplearning4j.nn.conf.NeuralNetConfiguration
//import org.deeplearning4j.nn.conf.layers.DenseLayer
//import org.deeplearning4j.nn.conf.layers.OutputLayer
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
//import org.deeplearning4j.nn.weights.WeightInit
//import org.nd4j.linalg.activations.Activation
//import org.nd4j.linalg.dataset.{DataSet, MiniBatchFileDataSetIterator}
//import org.nd4j.linalg.factory.Nd4j
//import org.nd4j.linalg.learning.config.Nesterovs
//import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
//
//object DQNTest extends App {
//  val seed = 123
//  val learningRate = 0.01
//  val batchSize = 50
//  val nEpochs = 30
//
//  val numInputs = 2
//  val numOutputs = 2
//  val numHiddenNodes = 20
//
//  val conf = new NeuralNetConfiguration
//    .Builder()
//    .seed(123)
//    .updater(
//      new Nesterovs(0.0001, 0.9)
//    )
//    .list
//    .layer(
//      0,
//      new DenseLayer
//        .Builder()
//        .nIn(numInputs)
//        .nOut(numHiddenNodes)
//        .weightInit(WeightInit.XAVIER)
//        .activation(Activation.RELU)
//        .build
//    )
//    .layer(
//      1,
//      new OutputLayer
//        .Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
//        .weightInit(WeightInit.XAVIER)
//        .activation(Activation.SOFTMAX)
//        .nIn(numHiddenNodes)
//        .nOut(numOutputs)
//        .build
//    )
//    .pretrain(false)
//    .backprop(true)
//    .build
//
//  val model = new MultiLayerNetwork(conf)
//
//  val dQn = DQN.getDQN(model)
//
//  dQn.init
////
//  val input = Nd4j.create(Array(1.0, -1.0))
//  val output = Nd4j.create(Array(0.0, 0.0))
//
//  val features = List.fill(10000)(input)
//  val labels = List.fill(10000)(output)
//
////  val dataSet = new DataSet(
////    Nd4j.concat(0, features:_*),
////    Nd4j.concat(0, labels:_*)
////  )
////  val dataSetIterator = new MiniBatchFileDataSetIterator(dataSet, 128)
//
//  dQn.fit(Nd4j.concat(0, features:_*), Nd4j.concat(0, labels:_*))
//
//  println(dQn.eval(Nd4j.concat(0, features:_*)))
//}