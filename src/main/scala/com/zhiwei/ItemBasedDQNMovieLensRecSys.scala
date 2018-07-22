package com.zhiwei

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.zhiwei.configs.datasetconfigs.{DataSetConfigT, MovieLensLargeDataSetConfig}
import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.{ItemBasedDQNMovieLensRLConfig, RLConfigT}
import com.zhiwei.configs.trainerconfigs.{ItemBasedDQNMovieLensTrainerConfig, TrainerConfigT}
import com.zhiwei.configs.{networkconfigs, recsysconfigs, replayqueueconfigs}
import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
import com.zhiwei.macros.nnmacros.ItemBasedDQNMovieLensNNMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.stateencoders.NGramStateEncoder
import com.zhiwei.rl.trainers.movielens.ItemBasedDQNMovieLensTrainerActor
import com.zhiwei.rl.policys.movielens.ItemBasedDoubleDQNMovieLensMovieLensPolicy
import com.zhiwei.rl.rewardfunctions.movielens.hitMovieNotNegReward
import com.zhiwei.rl.trainers.movielens.apis.SynchronousMovieLensDQNTrainerActorT.TrainRequest
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieId
import com.zhiwei.types.rltypes.DQNType.{History, Reward}
import com.zhiwei.utils.loadNetwork

object ItemBasedDQNMovieLensRecSys extends App {
  val dataSetConfig: DataSetConfigT = MovieLensLargeDataSetConfig
  val recSysConfig: RecSysConfigT = recsysconfigs.Config_1
  val networkConfig: NetworkConfigT = networkconfigs.SynItemBasedMovieLensDQNConfig
  val replayQueueConfig: ReplayQueueConfigT = replayqueueconfigs.Config_1
  val rLConfig: RLConfigT = ItemBasedDQNMovieLensRLConfig
  val trainerConfig: TrainerConfigT = ItemBasedDQNMovieLensTrainerConfig

  val rewardFunction: (List[MovieId], History) =>  Reward =
    hitMovieNotNegReward(recSysConfig.ratingThreshold)

  val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro
  val rLMacro =
    new ItemBasedDQNMovieLensNGramRLMacro(
      dataSetMacro.numObservationEntry,
      rLConfig.N,
      NGramStateEncoder.getNextState,
      rewardFunction
    )
  val nNMacro =
    new ItemBasedDQNMovieLensNNMacro(
      networkConfig,
      dataSetMacro,
      rLConfig
    )

  val dQN = loadNetwork(networkConfig.fileName, nNMacro.nnConfig)
  dQN.init()

  val trainSystemName = "ItemBasedMovieLensDQNRecSys"
  val trainSystem = ActorSystem(trainSystemName)

  val dQNPolicy: ItemBasedDoubleDQNMovieLensMovieLensPolicy =
    new ItemBasedDoubleDQNMovieLensMovieLensPolicy(
      recSysConfig,
      dataSetMacro,
      rLConfig,
      dQN,
      dQN.clone
    )

  val trainerActorRef: ActorRef =
    trainSystem.actorOf(
      ItemBasedDQNMovieLensTrainerActor.props(
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        rLConfig,
        networkConfig,
        dQNPolicy
      ),
      "ItemBasedMovieLensDQNTrainerActor"
    )

  trainerActorRef ! TrainRequest

  Await.ready(trainSystem.whenTerminated, Duration.Inf)

}