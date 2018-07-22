package com.zhiwei.types.datasettypes.movielens

trait MovieLensDataSetBaseTypeT {
  type MovieId = Int
  type MovieIdx = Int

  type Rating = Double

  type RatingThreshold = Double

  type TimeStamp = Long
}
