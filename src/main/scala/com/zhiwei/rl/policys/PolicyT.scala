package com.zhiwei.rl.policys

import com.zhiwei.rl.networks.Network
import com.zhiwei.types.rltypes.RLBaseTypeT.{Reward, State}
import org.nd4j.linalg.api.ndarray.INDArray

trait PolicyT[Action] {

  def fit(input: INDArray, output: INDArray): Double

  def fit(
           states: List[State],
           actions: List[Action],
           rewards: List[Reward],
           nextStates: List[State]
         ): Double

  def getNextAction(currentState: State): (Action, INDArray, Boolean)

  def saveNetwork(fileName: String): Unit

  def updateNetwork(
                     network_1: Option[Network],
                     network_2: Option[Network]
                   ): Unit

  def getPolicy: PolicyT[Action]

  def setEpsilon(newEpsilon: Double): Unit
}