package com.zhiwei.macros.datasetmacros.movielens

object MovieLensLargeDataSetMacro extends MovieLensDataSetMacro {
  val dBName = "MovieLensLarge"
  val moviesFilePath = "ExternalDataset/MovieLensLarge/movies.csv"
  val ratingsFilePath = "ExternalDataset/MovieLensLarge/ratings.csv"
  val movieTagsFilePath = "ExternalDataset/MovieLensLarge/tags.csv"
  val genomeTagsFilePath = "ExternalDataset/MovieLensLarge/genome-tags.csv"
  val genomeScoresFilePath = "ExternalDataset/MovieLensLarge/genome-scores.csv"

  val saveBatchSize = 4

  val TIME_DEBUG = false
}