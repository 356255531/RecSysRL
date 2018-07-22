package com.zhiwei.rl.trainers

import akka.actor.ActorRef

trait TrainerT {
  val trainerName: String

  val numWorker: Int
  val workerRouterActorRef: ActorRef

  // To determine when training ends
  var numTrainEpisode = 0
  val numTrainEpisodeThreshold = 10000000
  def ifLearnTerminated: Boolean =
    numTrainEpisode > numTrainEpisodeThreshold
}