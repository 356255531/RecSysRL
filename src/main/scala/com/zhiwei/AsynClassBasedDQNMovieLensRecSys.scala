//package com.zhiwei
//
//import akka.actor.{ActorRef, ActorSystem}
//
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration
//import com.zhiwei.configs.clusterconfig.ClusterConfigT
//import com.zhiwei.configs.datasetconfigs.{DataSetConfigT, MovieLensLargeDataSetConfig}
//import com.zhiwei.configs.{clusterconfigs, networkconfigs, recsysconfigs, replayqueueconfigs}
//import com.zhiwei.configs.networkconfigs.NetworkConfigT
//import com.zhiwei.configs.recsysconfigs.RecSysConfigT
//import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
//import com.zhiwei.configs.rlconfigs.{ClassBasedDQNMovieLensRLConfig, RLConfigT}
//import com.zhiwei.configs.trainerconfigs.{AsynClassBasedDQNMovieLensTrainerConfig, TrainerConfigT}
//import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
//import com.zhiwei.macros.nnmacros.ClassBasedDQNMovieLensNNMacro
//import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
//import com.zhiwei.rl.stateencoders.PosNegNGramStateEncoder
//import com.zhiwei.rl.trainers.TrainerT.TrainRequest
//import com.zhiwei.rl.trainers.movielens.AsynchronousClassBasedDQNMovieLensTrainerActor
//import com.zhiwei.rl.rewardfunctions.movielens.hitMovieReward
//import com.zhiwei.utils.loadNetwork
//import com.zhiwei.types.rltypes.movielens.ClassBasedDQNMovieLensRLType.{History, Reward}
//import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieId
//
//object AsynClassBasedDQNMovieLensRecSys extends App {
//  val dataSetConfig: DataSetConfigT = MovieLensLargeDataSetConfig
//  val recSysConfig: RecSysConfigT = recsysconfigs.Config_1
//  val networkConfig: NetworkConfigT = networkconfigs.AsynClassBasedMovieLensDQNConfig
//  val replayQueueConfig: ReplayQueueConfigT = replayqueueconfigs.Config_1
//  val rLConfig: RLConfigT = ClassBasedDQNMovieLensRLConfig
//  val trainerConfig: TrainerConfigT = AsynClassBasedDQNMovieLensTrainerConfig
//  val clusterConfig: ClusterConfigT = clusterconfigs.Config_1
//
//  val rewardFunction: (List[MovieId], History) =>  Reward =
//    hitMovieReward(recSysConfig.ratingThreshold)
//
//  val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro
//  val rLMacro =
//    new ClassBasedDQNMovieLensNGramRLMacro(
//      dataSetMacro.numObservationEntry,
//      rLConfig.N,
//      PosNegNGramStateEncoder.getNextState,
//      rewardFunction
//    )
//  val nNMacro =
//    new ClassBasedDQNMovieLensNNMacro(
//      clusterConfig,
//      networkConfig,
//      dataSetMacro,
//      rLConfig
//    )
//
//  val dqn = loadNetwork(networkConfig.fileName, nNMacro.nnConfig)
//  dqn.init()
//
//  val trainSystemName = "AsynClassBasedMovieLensDQNRecSys"
//  val trainSystem = ActorSystem(trainSystemName)
//
//  val trainerActorRef: ActorRef =
//    trainSystem.actorOf(
//      AsynchronousClassBasedDQNMovieLensTrainerActor.props(
//        rLConfig,
//        clusterConfig,
//        recSysConfig,
//        networkConfig,
//        replayQueueConfig,
//        dataSetMacro,
//        rLMacro,
//        dqn
//      ),
//      "AsynClassBasedMovieLensDQNTrainerActor"
//    )
//
//  trainerActorRef ! TrainRequest
//
//  Await.ready(trainSystem.whenTerminated, Duration.Inf)
//
//}