package com.zhiwei.macros.datasetmacros.movielens

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import scala.collection.JavaConverters._

trait MovieLensDataSetMacro {

  // DB and Collection Names
  val dBName: String

  val ratingsCollectionName: String = "Ratings"
  val moviesCollectionName: String = "Movies"

  val genomeTagsCollectionName: String = "GenomeTags"
  val genomeScoresCollectionName: String = "GenomeScores"

  val reducedRatingsCollectionName: String = "ReducedRatings" // Only the ratings with genome scores
  val reducedMoviesCollectionName: String = "ReducedMovies"   // Only the movies with genome scores and exists in reducedRatingsCollection
  val enhancedMoviesCollectionName: String = "EnhancedMovies" // add numRating, averageRating, genomeScores to movies

  val movieFeaturesCollectionName: String = "MovieFeatures" // movies with feature vectors
  val movieClassFeaturesCollectionName: String = "MovieClassFeatures"
  val userHistoriesCollectionName: String = "UserHistories"

  // Feature vector entry numbers
  val numAverageRatingFeatureEntry: Int = 1
  val numGenomeScoresFeature: Int = 1128
  val numNumRatingFeatureEntry: Int = 5
  val numYearFeatureEntry: Int = 6

  val numMovieContentFeatures: Int =
    numNumRatingFeatureEntry + numYearFeatureEntry + numAverageRatingFeatureEntry + numGenomeScoresFeature

  val numUserRatingFeatureEntry: Int = 1
  val numObservationEntry: Int = numMovieContentFeatures + numUserRatingFeatureEntry

  val numReducedMovies: Int = 10370

  // Feature vectors default values
  val defaultAverageRatingFeatureArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](numAverageRatingFeatureEntry)(0).asJava
    )
  val defaultGenomeScoresFeatureArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](numGenomeScoresFeature)(0).asJava
    )
  val defaultNumRatingFeatureArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](numNumRatingFeatureEntry)(0).asJava
    )
  val defaultYearFeatureArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](numYearFeatureEntry)(0).asJava
    )

  val defaultMovieFeatureArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](numMovieContentFeatures)(0).asJava
    )

  val defaultUserRatingFeatureArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](numUserRatingFeatureEntry)(0).asJava
    )

  val defaultObservationArrayList: java.util.ArrayList[Double] =
    new java.util.ArrayList[Double](
      List.fill[Double](
        numObservationEntry
      )(0).asJava
    )

  val defaultObservation: INDArray =
    Nd4j.create(
      Array.fill[Double](numObservationEntry)(0)
    )


  // File path
  val moviesFilePath: String
  val ratingsFilePath: String
  val movieTagsFilePath: String
  val genomeTagsFilePath: String
  val genomeScoresFilePath: String

  // Asynchronous saving batch size
  val saveBatchSize: Int

  // Indexes for all collections
  val emptyIndexes: List[String] = List()
  val ratingsCollectionAscendIndexes: List[String] =  List("userId", "movieId", "timestamp")
  val ratingsCollectionDescendIndexes: List[String] =  emptyIndexes
  val moviesCollectionAscendIndexes: List[String] = List("movieId")
  val moviesCollectionDescendIndexes: List[String] = emptyIndexes
  val genomeTagsCollectionAscendIndexes: List[String] = List("tagId")
  val genomeTagsCollectionDescendIndexes: List[String] = emptyIndexes
  val genomeScoresCollectionAscendIndexes: List[String] = List("tagId", "movieId")
  val genomeScoresCollectionDescendIndexes: List[String] = emptyIndexes
  val reducedMoviesCollectionAscendIndexes: List[String] = List("movieId", "movieIdx")
  val reducedMoviesCollectionDescendIndexes: List[String] = emptyIndexes
  val reducedRatingsCollectionAscendIndexes: List[String] = List("userId", "movieId", "timestamp")
  val reducedRatingsCollectionDescendIndexes: List[String] = emptyIndexes
  val enhancedMoviesCollectionAscendIndexes: List[String] = List("movieId")
  val enhancedMoviesCollectionDescendIndexes: List[String] = emptyIndexes
  val movieFeaturesCollectionAscendIndexes: List[String] = List("movieId", "movieIdx", "featureVector")
  val movieFeaturesCollectionDescendIndexes: List[String] = emptyIndexes
  val movieClassFeaturesCollectionAscendIndexes: List[String] = List("movieId", "class")
  val movieClassFeaturesCollectionDescendIndexes: List[String] = emptyIndexes
  val userHistoriesCollectionAscendIndexes: List[String] = List("userId")
  val userHistoriesCollectionDescendIndexes: List[String] = emptyIndexes

  // Debug symbol
  val TIME_DEBUG: Boolean
}
