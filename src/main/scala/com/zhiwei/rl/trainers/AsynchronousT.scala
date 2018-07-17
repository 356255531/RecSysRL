package com.zhiwei.rl.trainers

trait AsynchronousT {
  val numAgent = Runtime.getRuntime.availableProcessors()
}
