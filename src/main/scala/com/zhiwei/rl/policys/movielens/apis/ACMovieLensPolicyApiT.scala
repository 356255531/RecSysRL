package com.zhiwei.rl.policys.movielens.apis

import com.zhiwei.rl.networks.Network
import com.zhiwei.types.rltypes.RLBaseTypeT.{Reward, State}
import org.deeplearning4j.nn.gradient.Gradient
import org.nd4j.linalg.api.ndarray.INDArray

trait ACMovieLensPolicyApiT[Action]  extends MovieLensPolicyT[Action] {
  val actorNetwork: Network
  actorNetwork.init()

  val criticNetwork: Network
  criticNetwork.init()

  def saveActorCriticNetwork(savePath: String): Unit = {
    actorNetwork.save(savePath + "/actorNetwork.zip")
    criticNetwork.save(savePath + "/actorNetwork.zip")
  }

  def initActorCriticNetwork(): Unit = {
    if (!actorNetwork.ifInit) actorNetwork.init()
    if (!criticNetwork.ifInit) criticNetwork.init()
  }

  def getGradients(
                    states: List[State],
                    actions: List[Action],
                    rewards: List[Reward]
                  ): (Gradient, Gradient, Int)

  def applyGradients(
                      policyGradient: Gradient,
                      valueGradient: Gradient,
                      batchSize: Int
                    ): Unit

  def evalActor(input: INDArray): INDArray =
    actorNetwork.eval(input)

  def evalCritic(input: INDArray): INDArray =
    criticNetwork.eval(input)
}