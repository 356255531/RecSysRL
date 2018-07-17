package com.zhiwei

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.configs.datasetconfigs.{DataSetConfigT, MovieLensLargeDataSetConfig}
import com.zhiwei.configs.{clusterconfigs, dqnconfigs, recsysconfigs, replayqueueconfigs}
import com.zhiwei.configs.dqnconfigs.DQNConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.{ClassBasedDQNMovieLensRLConfig, RLConfigT}
import com.zhiwei.configs.trainerconfigs.{ClassBasedDQNMovieLensTrainerConfig, TrainerConfigT}
import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
import com.zhiwei.macros.nnmacros.ClassBasedDQNMovieLensNNMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.policys.movielens.ClassBasedDQNMovieLensPolicy
import com.zhiwei.rl.stateencoders.PosNegNGramStateEncoder
import com.zhiwei.rl.trainers.TrainerT.TrainRequest
import com.zhiwei.rl.trainers.dqntrainers.ClassBasedDQNMovieLensTrainerActor
import com.zhiwei.rl.rewardfunctions.movielens.hitMovieReward
import com.zhiwei.utils.loadNetwork
import com.zhiwei.types.rltypes.movielens.ClassBasedDQNMovieLensRLType.{History, Reward}
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIds

object ClassBasedDQNMovieLensRecSys extends App {
  val dataSetConfig: DataSetConfigT = MovieLensLargeDataSetConfig
  val recSysConfig: RecSysConfigT = recsysconfigs.Config_1
  val dQNConfig: DQNConfigT = dqnconfigs.Config_1
  val replayQueueConfig: ReplayQueueConfigT = replayqueueconfigs.Config_1
  val rLConfig: RLConfigT = ClassBasedDQNMovieLensRLConfig
  val trainerConfig: TrainerConfigT = ClassBasedDQNMovieLensTrainerConfig
  val clusterConfig: ClusterConfigT = clusterconfigs.Config_1

  val rewardFunction: (MovieIds, History) =>  Reward =
    hitMovieReward(recSysConfig.ratingThreshold)

  val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro
  val rLMacro =
    new ClassBasedDQNMovieLensNGramRLMacro(
      dataSetMacro.numObservationEntry,
      rLConfig.N,
      PosNegNGramStateEncoder.getNextState,
      rewardFunction
    )
  val nNMacro =
    new ClassBasedDQNMovieLensNNMacro(
      clusterConfig,
      dQNConfig,
      dataSetMacro,
      rLConfig
    )

  val dqn = loadNetwork(nNMacro.filePath, nNMacro.nnConfig)

  val trainSystemName = "ClassBasedMovieLensDQNRecSys"
  val trainSystem = ActorSystem(trainSystemName)

  val dQNPolicy: ClassBasedDQNMovieLensPolicy =
    new ClassBasedDQNMovieLensPolicy(
      rLConfig,
      clusterConfig,
      dataSetMacro,
      dqn
    )

  val trainerActorRef: ActorRef =
    trainSystem.actorOf(
      ClassBasedDQNMovieLensTrainerActor.props(
        rLConfig,
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        dQNPolicy
      ),
      "ClassBasedMovieLensDQNTrainerActor"
    )

  trainerActorRef ! TrainRequest

  Await.ready(trainSystem.whenTerminated, Duration.Inf)

}