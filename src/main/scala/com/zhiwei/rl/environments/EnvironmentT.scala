package com.zhiwei.rl.environments

import com.zhiwei.types.rltypes.RLBaseTypeT.Observation

trait EnvironmentT {
  var done: Boolean

  def init(): Observation

  def step[Action](action: Action): Any
}