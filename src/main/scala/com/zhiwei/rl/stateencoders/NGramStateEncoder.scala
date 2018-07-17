package com.zhiwei.rl.stateencoders

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.indexing.NDArrayIndex

object NGramStateEncoder extends {
  def getNextState(
                  state: INDArray,
                  observation: INDArray
                  ): INDArray = {
    val stateShape = state.shape
    val observationShape = observation.shape
    if (stateShape(1) != observationShape(1)) {
      println(
        "The shapes of state and observation are infeasible to concatenate!"
      )
      println("state shape is ")
      stateShape.foreach(print)
      println("observation shape is ")
      observationShape.foreach(print)
      throw new IllegalArgumentException
    }

    val concatedRows = Nd4j
      .concat(
        0,
        state,
        observation
      )
    concatedRows.get(
      NDArrayIndex.interval(1, stateShape(0)+1),
      NDArrayIndex.all()
    )
  }
}
