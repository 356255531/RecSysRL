package com.zhiwei.rl.stateencoders

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.indexing.NDArrayIndex

object PosNegNGramStateEncoder extends {
  def getNextState(
                    state: INDArray,
                    observation: INDArray,
                    rating: Double,
                    ratingThreshold: Double
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

    val N = stateShape(0)

    val posObservation =
      state.get(
        NDArrayIndex.interval(0, N / 2),
        NDArrayIndex.all()
      )

    val negObservation =
      state.get(
        NDArrayIndex.interval(N / 2, N),
        NDArrayIndex.all()
      )

    if (rating >= ratingThreshold) {
      val newPosObservation =
        Nd4j
          .concat(
            0,
            posObservation,
            observation
          )
          .get(
            NDArrayIndex.interval(0, N / 2),
            NDArrayIndex.all()
          )

      Nd4j.concat(0, newPosObservation, negObservation)
    }
    else {
      val newNegObservation =
        Nd4j
          .concat(
            0,
            negObservation,
            observation
          )
          .get(
            NDArrayIndex.interval(0, N / 2),
            NDArrayIndex.all()
          )

      Nd4j.concat(0, posObservation, newNegObservation)
    }
  }
}

