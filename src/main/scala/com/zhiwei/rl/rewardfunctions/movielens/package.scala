package com.zhiwei.rl.rewardfunctions

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.RatingThreshold
import com.zhiwei.types.dbtypes.DBBaseType.Documents
import com.zhiwei.types.rltypes.RLBaseType.{History, Reward}

package object movielens {
  def hitMovieReward(ratingThreshold: RatingThreshold)(
    recommendedMovieIds: List[Long],
    unvisitedDocs: History
  ): Reward = {

    val recommendedMovieIdSet: Set[Long] = recommendedMovieIds.toSet

    val relatedMovieIdSet =
      unvisitedDocs
          .filter(_.get("rating", classOf[java.lang.Double]).toDouble > ratingThreshold)
          .map(_.get("movieId", classOf[java.lang.Long]).toLong)
          .toSet

    (recommendedMovieIdSet intersect relatedMovieIdSet).size
  }
}
