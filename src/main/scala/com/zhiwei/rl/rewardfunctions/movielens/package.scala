package com.zhiwei.rl.rewardfunctions

import com.zhiwei.types.datasettypes.movielens.MovieLensDataSetBaseType.{RatingThreshold, MovieIdx}
import com.zhiwei.types.rltypes.RLBaseTypeT.{History, Reward}

package object movielens {
  def hitMovieReward(ratingThreshold: RatingThreshold)(
    recommendedMovieIndexes: List[MovieIdx],
    unvisitedDocs: History
  ): Reward = {

    val recommendedMovieIdSet: Set[MovieIdx] = recommendedMovieIndexes.toSet

    val relevantMovieIdSet: Set[MovieIdx] =
      unvisitedDocs
          .filter(_.get("rating", classOf[java.lang.Double]).toDouble > ratingThreshold)
          .map(_.get("movieIdx", classOf[java.lang.Integer]).toInt)
          .toSet

    (recommendedMovieIdSet intersect relevantMovieIdSet).size
  }

  def hitMovieNotNegReward(ratingThreshold: RatingThreshold)(
    recommendedMovieIndexes: List[MovieIdx],
    unvisitedDocs: History
  ): Reward = {

    val recommendedMovieIdSet: Set[MovieIdx] = recommendedMovieIndexes.toSet

    val relevantMovieIdSet: Set[MovieIdx] =
      unvisitedDocs
        .filter(_.get("rating", classOf[java.lang.Double]).toDouble > ratingThreshold)
        .map(_.get("movieIdx", classOf[java.lang.Integer]).toInt)
        .toSet

    val unrelevantMovieIdSet: Set[MovieIdx] =
      unvisitedDocs
        .filter(_.get("rating", classOf[java.lang.Double]).toDouble <= ratingThreshold)
        .map(_.get("movieIdx", classOf[java.lang.Integer]).toInt)
        .toSet

    2 * (recommendedMovieIdSet intersect relevantMovieIdSet).size - (recommendedMovieIdSet intersect unrelevantMovieIdSet).size
  }
}
