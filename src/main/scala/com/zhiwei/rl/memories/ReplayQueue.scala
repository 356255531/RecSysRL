package com.zhiwei.rl.memories

import scala.collection.mutable

class ReplayQueue[Transition](
                               queueSize: Int,
                               minGetBatchSize: Int
                             ) {

  var memoryQueue: mutable.Queue[Transition] = mutable.Queue()

  def getTransitionBatchOption(batchSize: Int): Option[List[Transition]] = {
    if (memoryQueue.size < minGetBatchSize) None
    else {
      val realBatchSize: Int =
        if (memoryQueue.size > batchSize) batchSize
        else memoryQueue.size
      val randomSeed = new scala.util.Random(System.currentTimeMillis)

      val retBatch =
        Some(
          List.fill(realBatchSize)(memoryQueue(randomSeed.nextInt(memoryQueue.size)))
        )
      (0 until realBatchSize / 4)
        .foreach(idx => memoryQueue.dequeue())
      retBatch

    }
  }

  def enqueueTransition(transition: Transition): Unit = {
    if (memoryQueue.size > queueSize) memoryQueue.dequeue()
    memoryQueue.enqueue(transition)
  }
}