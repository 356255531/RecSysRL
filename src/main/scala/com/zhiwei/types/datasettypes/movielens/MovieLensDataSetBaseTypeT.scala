package com.zhiwei.types.datasettypes.movielens

trait MovieLensDataSetBaseTypeT {
  type MovieId = Long
  type MovieIds = List[MovieId]

  type Rating = Double
  type RatingThreshold = Double
}
