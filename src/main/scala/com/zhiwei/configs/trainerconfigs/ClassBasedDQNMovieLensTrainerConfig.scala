package com.zhiwei.configs.trainerconfigs

object ClassBasedDQNMovieLensTrainerConfig extends TrainerConfigT{
  val trainerMode = "Synchronous"
  val agentMode = "DQN"
  val ifTrain = true
}