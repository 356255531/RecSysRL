package com.zhiwei

import akka.actor.{ActorRef, ActorSystem}
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.configs.{clusterconfigs, networkconfigs, recsysconfigs, replayqueueconfigs}
import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.{ClassBasedDQNMovieLensRLConfig, RLConfigT}
import com.zhiwei.configs.trainerconfigs.{ClassBasedDQNMovieLensTrainerConfig, TrainerConfigT}
import com.zhiwei.macros.datasetmacros.movielens.{MovieLensDataSetMacro, MovieLensLargeDataSetMacro}
import com.zhiwei.macros.nnmacros.ClassBasedDQNMovieLensNNMacro
import com.zhiwei.macros.rlmacros.movielens.ClassBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.policys.movielens.ClassBasedDoubleDQNMovieLensMovieLensPolicy
import com.zhiwei.rl.stateencoders.NGramStateEncoder
import com.zhiwei.rl.trainers.movielens.ClassBasedDQNMovieLensTrainerActor
import com.zhiwei.rl.rewardfunctions.movielens.hitMovieReward
import com.zhiwei.rl.trainers.movielens.apis.SynchronousMovieLensDQNTrainerActorT.TrainRequest
import com.zhiwei.utils.loadNetwork
import com.zhiwei.types.rltypes.DQNType.{History, Reward}
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieId
import org.bson.Document

object ClassBasedDQNMovieLensRecSys extends App {
  val recSysConfig: RecSysConfigT = recsysconfigs.Config_1
  val networkConfig: NetworkConfigT = networkconfigs.SynClassBasedMovieLensDQNConfig
  val replayQueueConfig: ReplayQueueConfigT = replayqueueconfigs.Config_1
  val rLConfig: RLConfigT = ClassBasedDQNMovieLensRLConfig
  val trainerConfig: TrainerConfigT = ClassBasedDQNMovieLensTrainerConfig
  val clusterConfig: ClusterConfigT = clusterconfigs.Config_1

  val rewardFunction: (List[MovieId], History) =>  Reward =
    hitMovieReward(recSysConfig.ratingThreshold)

  val dataSetMacro: MovieLensDataSetMacro = MovieLensLargeDataSetMacro
  val rLMacro =
    new ClassBasedDQNMovieLensNGramRLMacro(
      dataSetMacro.numObservationEntry,
      rLConfig.N,
      NGramStateEncoder.getNextState,
      rewardFunction
    )
  val nNMacro =
    new ClassBasedDQNMovieLensNNMacro(
      clusterConfig,
      networkConfig,
      dataSetMacro,
      rLConfig
    )

  val dqn = loadNetwork(networkConfig.fileName, nNMacro.nnConfig)
  dqn.init()

  val trainSystemName = "ClassBasedMovieLensDQNRecSys"
  val trainSystem = ActorSystem(trainSystemName)

  val client: MongoClient = new MongoClient()
  val dBName: String = dataSetMacro.dBName
  val movieClassFeaturesCollectionName: String = dataSetMacro.movieClassFeaturesCollectionName
  val movieClassFeaturesCollection: MongoCollection[Document] =
    client.getDatabase(dBName).getCollection(movieClassFeaturesCollectionName)
  val dQNPolicy: ClassBasedDoubleDQNMovieLensMovieLensPolicy =
    new ClassBasedDoubleDQNMovieLensMovieLensPolicy(
      clusterConfig,
      recSysConfig,
      dataSetMacro,
      movieClassFeaturesCollection,
      rLConfig,
      dqn,
      dqn.clone
    )

  val trainerActorRef: ActorRef =
    trainSystem.actorOf(
      ClassBasedDQNMovieLensTrainerActor.props(
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        rLConfig,
        networkConfig,
        dQNPolicy
      ),
      "ClassBasedMovieLensDQNTrainerActor"
    )

  trainerActorRef ! TrainRequest

  Await.ready(trainSystem.whenTerminated, Duration.Inf)

}