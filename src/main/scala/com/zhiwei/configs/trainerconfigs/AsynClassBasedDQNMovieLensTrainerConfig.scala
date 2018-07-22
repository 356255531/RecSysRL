package com.zhiwei.configs.trainerconfigs

object AsynClassBasedDQNMovieLensTrainerConfig extends TrainerConfigT{
  val trainerMode = "Asynchronous"
  val agentMode = "DQN"
  val ifTrain = true
}