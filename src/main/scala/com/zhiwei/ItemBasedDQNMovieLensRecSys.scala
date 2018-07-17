package com.zhiwei

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.zhiwei.configs.datasetconfigs.{DataSetConfigT, MovieLensLargeDataSetConfig}
import com.zhiwei.configs.dqnconfigs.DQNConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.{ItemBasedDQNMovieLensRLConfig, RLConfigT}
import com.zhiwei.configs.trainerconfigs.{ItemBasedDQNMovieLensTrainerConfig, TrainerConfigT}
import com.zhiwei.configs.{dqnconfigs, recsysconfigs, replayqueueconfigs}
import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
import com.zhiwei.macros.nnmacros.ItemBasedDQNMovieLensNNMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.stateencoders.PosNegNGramStateEncoder
import com.zhiwei.rl.trainers.TrainerT.TrainRequest
import com.zhiwei.rl.trainers.dqntrainers.ItemBasedDQNMovieLensTrainerActor
import com.zhiwei.rl.policys.movielens.ItemBasedDQNMovieLensPolicy
import com.zhiwei.rl.rewardfunctions.movielens.hitMovieReward
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIds
import com.zhiwei.types.rltypes.movielens.ItemBasedDQNMovieLensRLType.{History, Reward}
import com.zhiwei.utils.loadNetwork

object ItemBasedDQNMovieLensRecSys extends App {
  val dataSetConfig: DataSetConfigT = MovieLensLargeDataSetConfig
  val recSysConfig: RecSysConfigT = recsysconfigs.Config_1
  val dQNConfig: DQNConfigT = dqnconfigs.Config_1
  val replayQueueConfig: ReplayQueueConfigT = replayqueueconfigs.Config_1
  val rLConfig: RLConfigT = ItemBasedDQNMovieLensRLConfig
  val trainerConfig: TrainerConfigT = ItemBasedDQNMovieLensTrainerConfig

  val rewardFunction: (MovieIds, History) =>  Reward =
    hitMovieReward(recSysConfig.ratingThreshold)

  val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro
  val rLMacro =
    new ItemBasedDQNMovieLensNGramRLMacro(
      dataSetMacro.numObservationEntry,
      rLConfig.N,
      PosNegNGramStateEncoder.getNextState,
      rewardFunction
    )
  val nNMacro =
    new ItemBasedDQNMovieLensNNMacro(
      dQNConfig,
      dataSetMacro,
      rLConfig
    )

  val dqn = loadNetwork(nNMacro.filePath, nNMacro.nnConfig)

  val trainSystemName = "ItemBasedMovieLensDQNRecSys"
  val trainSystem = ActorSystem(trainSystemName)

  val dQNPolicy = new ItemBasedDQNMovieLensPolicy(rLConfig, recSysConfig, dataSetMacro, dqn)

  val trainerActorRef: ActorRef =
    trainSystem.actorOf(
      ItemBasedDQNMovieLensTrainerActor.props(
        rLConfig,
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        dQNPolicy
      ),
      "ItemBasedMovieLensDQNTrainerActor"
    )

  trainerActorRef ! TrainRequest

  Await.ready(trainSystem.whenTerminated, Duration.Inf)

}