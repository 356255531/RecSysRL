package com.zhiwei.rl.environments

import com.zhiwei.types.rltypes.RLBaseType.Observation

trait EnvironmentT[Action] {
  var done: Boolean

  def init(): Observation

  def step(action: Action): Any
}