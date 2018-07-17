package com.zhiwei.configs.trainerconfigs

object ItemBasedDQNMovieLensTrainerConfig extends TrainerConfigT{
  val trainerMode = "Synchronous"
  val agentMode = "DQN"
  val ifTrain = true
}