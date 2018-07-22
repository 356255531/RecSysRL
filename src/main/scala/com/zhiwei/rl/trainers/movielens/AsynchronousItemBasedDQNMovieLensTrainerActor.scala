package com.zhiwei.rl.trainers.movielens

import akka.actor.{ActorRef, Props}
import akka.routing.BalancingPool
import org.deeplearning4j.nn.gradient.Gradient
import com.zhiwei.configs.networkconfigs.NetworkConfigT
import com.zhiwei.configs.recsysconfigs.RecSysConfigT
import com.zhiwei.configs.replayqueueconfigs.ReplayQueueConfigT
import com.zhiwei.configs.rlconfigs.RLConfigT
import com.zhiwei.configs.clusterconfig.ClusterConfigT
import com.zhiwei.macros.datasetmacros.movielens.MovieLensDataSetMacro
import com.zhiwei.macros.rlmacros.movielens.ItemBasedDQNMovieLensNGramRLMacro
import com.zhiwei.rl.agents.movielens.AsynchronousItemBasedDQNMovieLensAgentWorkerActor
import com.zhiwei.rl.networks.Network
import com.zhiwei.rl.trainers.movielens.apis.AsynchronousMovieLensDQNTrainerActorT
import com.zhiwei.types.rltypes.DQNType.{Action, Done, Reward}


object AsynchronousItemBasedDQNMovieLensTrainerActor{
  final case class LearnRequest(dQNNetwork: Network)
  final case object GlobalShareCounterIncreaseRequest
  final case object GetDQNNetworkRequest
  final case class GetDQNNetworkResult(dQNNetwork: Network, globalSharedCounter: Long)
  final case class PushGradientRequest(gradient: Gradient, batchSize: Int)
  final case object PushGradientSuccess
  final case class StepLearnResult(
                                    actorName: String,
                                    reward: Reward,
                                    action: Action,
                                    done: Done,
                                    episode: Int,
                                    epsilon: Double,
                                    trainLoss: Double,
                                    precision: Double,
                                    recall: Double,
                                    optimalPrecision: Double,
                                    optimalRecall: Double,
                                    maxPrecision: Double,
                                    maxRecall: Double
                                  )
  final case class EpisodeLearnResult(
                                       actorName: String,
                                       episode: Int,
                                       epsilon: Double,
                                       stepReward: Double,
                                       averagePrecision: Double,
                                       averageRecall: Double,
                                       maxPrecision: Double,
                                       maxRecall: Double,
                                       averageOptimalPrecision: Double,
                                       averageOptimalRecall: Double,
                                       maxOptimalPrecision: Double,
                                       maxOptimalRecall: Double,
                                       averageMaxSingleStepPrecision: Double,
                                       averageMaxSingleStepRecall: Double,
                                       maxMaxSingleStepPrecision: Double,
                                       maxMaxSingleStepRecall: Double
                                     )
  final case object SaveNetworkRequest

  def props(
             clusterConfig: ClusterConfigT,
             recSysConfig: RecSysConfigT,
             replayQueueConfig: ReplayQueueConfigT,
             dataSetMacro: MovieLensDataSetMacro,
             rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
             rLConfig: RLConfigT,
             networkConfig: NetworkConfigT,
             onlineNetwork: Network,
             targetNetwork: Network
           ): Props = {
    Props(
      new AsynchronousItemBasedDQNMovieLensTrainerActor(
        clusterConfig,
        recSysConfig,
        replayQueueConfig,
        dataSetMacro,
        rLMacro,
        rLConfig,
        networkConfig,
        onlineNetwork,
        targetNetwork
      )
    )
  }
}

class AsynchronousItemBasedDQNMovieLensTrainerActor(
                                                      clusterConfig: ClusterConfigT,
                                                      recSysConfig: RecSysConfigT,
                                                      replayQueueConfig: ReplayQueueConfigT,
                                                      dataSetMacro: MovieLensDataSetMacro,
                                                      rLMacro: ItemBasedDQNMovieLensNGramRLMacro,
                                                      override val rLConfig: RLConfigT,
                                                      override val networkConfig: NetworkConfigT,
                                                      override val onlineNetwork: Network,
                                                      override var targetNetwork: Network
                                                    )
  extends AsynchronousMovieLensDQNTrainerActorT[Action] {

  val trainerName = "AsynchronousItemBasedDQNMovieLensTrainer"

  val globalShareCounter: Long = 0

  val numWorker: Int = Runtime.getRuntime.availableProcessors
  val workerRouterActorRef: ActorRef = context.actorOf(
    BalancingPool(numWorker).props(
      AsynchronousItemBasedDQNMovieLensAgentWorkerActor.props(
        clusterConfig,
        onlineNetwork,
        self,
        rLConfig,
        recSysConfig,
        rLMacro,
        dataSetMacro
      )
    )
    ,
    "AsynchronousItemBasedDQNMAgentRouter"
  )
}