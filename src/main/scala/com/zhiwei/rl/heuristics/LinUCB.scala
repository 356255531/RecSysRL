package com.zhiwei.rl.heuristics

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.inverse.InvertMatrix

import com.zhiwei.configs.rlconfigs.RLConfigT

class LinUCB(
              numArm: Int,
              numObservationFeatureEntries: Int,
              numActionFeatureEntries: Int,
              var epsilon: Double,
              rLConfig: RLConfigT
            ) {
  val oneStateActionPairEntryNum: Int = numObservationFeatureEntries + numActionFeatureEntries
  println("fuck1")
  var AMatrixeArray: Array[INDArray] =
    Array.fill(numArm)(Nd4j.eye(oneStateActionPairEntryNum * rLConfig.N))
  println("fuck2")
  var bVectorArray: Array[INDArray] =
    Array.fill(numArm)(Nd4j.zeros(oneStateActionPairEntryNum * rLConfig.N, 1))
  var probArray: Array[Double] = Array.fill(numArm)(0.0)

  def setEpsilon(newEpsilon: Double): Unit = {
    epsilon = newEpsilon
  }

  def getNextAction(armFeatureVectors: List[INDArray]): Int = {
    armFeatureVectors
      .indices
      .foreach(
        idx => {
          val theta: INDArray = InvertMatrix.invert(AMatrixeArray(idx), false)
          val armFeatureVector: INDArray = armFeatureVectors(idx)
          probArray(idx) =
            theta.transpose().mul(armFeatureVector).getDouble(0) +
          scala.math.sqrt(
            armFeatureVector
              .transpose()
              .mul(InvertMatrix.invert(AMatrixeArray(idx), false))
              .mul(armFeatureVector).getDouble(0)
          ) * epsilon
        }
      )
    probArray.zipWithIndex.maxBy(_._1)._2
  }

  def updateLinUCB(
                    armIdx: Int,
                    armFeatureVector: INDArray,
                    qFunction: Double
                  ): Unit = {
    AMatrixeArray(armIdx) =
      AMatrixeArray(armIdx)
        .add(
          armFeatureVector.mul(InvertMatrix.invert(armFeatureVector, false))
        )

    bVectorArray(armIdx) = bVectorArray(armIdx).add(armFeatureVector.mul(qFunction))
  }
}