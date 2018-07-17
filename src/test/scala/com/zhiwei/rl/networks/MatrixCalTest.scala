package com.zhiwei.rl.networks

import org.nd4j.linalg.factory.Nd4j

object MatrixCalTest extends App {
  val a = Nd4j.create(Array(1, 2.0), Array(1, 2))
  val b = Nd4j.create(Array(3, 4.0), Array(2, 1))
  println(a.muli(b))
}
