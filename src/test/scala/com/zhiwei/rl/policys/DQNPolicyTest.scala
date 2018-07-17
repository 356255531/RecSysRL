//package com.zhiwei.rl.policys
//
//import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
//import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
//import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
//import org.deeplearning4j.nn.weights.WeightInit
//import org.nd4j.linalg.activations.Activation
//import org.nd4j.linalg.factory.Nd4j
//import org.nd4j.linalg.learning.config.Nesterovs
//import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
//import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
//import com.zhiwei.configs.rlconfigs.{ClassBasedDQNMovieLensRLConfig, RLConfigT}
//import com.zhiwei.rl.networks.DQN
//import com.zhiwei.rl.policys.PolicyT.{GetNextActionRequest, GetNextActionResult, PolicyInitRequest, PolicyInitSuccess}
//
//object DQNPolicyTest extends App {
//  class DQNPolicyTestActor extends Actor with ActorLogging{
//
//    val seed = 123
//    val learningRate = 0.01
//    val batchSize = 50
//    val nEpochs = 30
//
//    val numInputs = 2
//    val numOutputs = 2
//    val numHiddenNodes = 20
//
//    val conf: MultiLayerConfiguration = new NeuralNetConfiguration
//    .Builder()
//      .seed(123)
//      .updater(
//        new Nesterovs(0.0001, 0.9)
//      )
//      .list
//      .layer(
//        0,
//        new DenseLayer
//        .Builder()
//          .nIn(numInputs)
//          .nOut(numHiddenNodes)
//          .weightInit(WeightInit.XAVIER)
//          .activation(Activation.RELU)
//          .build
//      )
//      .layer(
//        1,
//        new OutputLayer
//        .Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
//          .weightInit(WeightInit.XAVIER)
//          .activation(Activation.SOFTMAX)
//          .nIn(numHiddenNodes)
//          .nOut(numOutputs)
//          .build
//      )
//      .pretrain(false)
//      .backprop(true)
//      .build
//
//    val model = new MultiLayerNetwork(conf)
//
//    val dQn: DQN = DQN.getDQN(model)
//
//    val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro
//
//    val rLConfig: RLConfigT = ClassBasedDQNMovieLensRLConfig
//
//    val policy: ActorRef = context.actorOf(
//      DQNPolicyActor.props(
//        dataSetMacro,
//        rLConfig,
//        dQn
//      )
//    )
//
//    override def receive: Receive = {
//      case PolicyInitRequest =>
//        log.info("fuck")
//        policy ! PolicyInitRequest
//        policy ! GetNextActionRequest(Nd4j.create(Array(1.0, 1.0)))
//      case PolicyInitSuccess =>
//        log.info("init succ!")
//      case GetNextActionResult(action) =>
//        log.info(action.toString)
//    }
//  }
//
//
//  val dQNTestActorSysName = "DQNTestActorSystem"
//
//  val dQNTestActorSystem = ActorSystem(dQNTestActorSysName)
//
//  val testActor =
//    dQNTestActorSystem.actorOf(
//      Props(
//        new DQNPolicyTestActor
//      )
//    )
//
//  testActor ! PolicyInitRequest
//}
