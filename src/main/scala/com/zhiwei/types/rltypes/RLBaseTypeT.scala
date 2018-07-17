package com.zhiwei.types.rltypes

import org.bson.Document
import org.nd4j.linalg.api.ndarray.INDArray

trait RLBaseTypeT {
  type Observation = INDArray

  type State = INDArray
  type Reward = Double
  type StepReward = Double
  type Done = Boolean

  // History
  type Record = Document
  type History = List[Record]
}
