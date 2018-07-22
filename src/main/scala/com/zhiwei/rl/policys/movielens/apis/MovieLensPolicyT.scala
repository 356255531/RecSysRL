package com.zhiwei.rl.policys.movielens.apis

import com.zhiwei.rl.policys.PolicyT
import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.MovieIdx
import com.zhiwei.types.rltypes.RLBaseTypeT.State
import org.nd4j.linalg.api.ndarray.INDArray

trait MovieLensPolicyT[Action] extends PolicyT[Action]{
  def getRecommendedMovieIndexes(
                                  action: Action,
                                  nNOutput: INDArray,
                                  isRandomAction: Boolean
                                ): List[MovieIdx]

  def getNextActionAndRecommendedMovieIndexes(
                                               currentState: State
                                             ): (Action, List[MovieIdx])
}