package com.zhiwei.types.rltypes

import org.bson.Document

import org.nd4j.linalg.api.ndarray.INDArray

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.{Rating, TimeStamp}

object RLBaseTypeT {
  type Observation = INDArray

  type State = INDArray
  type Reward = Double
  type Done = Boolean

  type StepReward = Double

  // History
  type Record = Document
  type History = List[Record]
}

trait RLBaseTypeT[A] {
  type Observation = INDArray

  type State = INDArray
  type Action = A
  type Reward = Double
  type Done = Boolean

  type StepReward = Double

  // History
  type Record = Document
  type History = List[Record]

  type Transition = (Array[Array[Double]], Action, Reward, Array[Array[Double]])
  type EnvStepReturn = (History, Observation, Rating, Done, TimeStamp)
 }
