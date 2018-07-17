package com.zhiwei.types.rltypes

trait RLTypeT[A] extends RLBaseTypeT {
  type Action = A

  type Transition = (State, Action, Reward, State)
  type Transitions = List[Transition]
}
