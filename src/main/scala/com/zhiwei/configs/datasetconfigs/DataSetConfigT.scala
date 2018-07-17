package com.zhiwei.configs.datasetconfigs

trait DataSetConfigT {
  val dataSetName: String
  val dataSetMode: String
  val forceRebuild: Boolean
}
