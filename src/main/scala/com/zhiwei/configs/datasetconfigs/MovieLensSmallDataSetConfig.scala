import com.zhiwei.configs.datasetconfigs.DataSetConfigT

object MovieLensSmallDataSetConfig extends DataSetConfigT {
  val dataSetName = "MovieLens"
  val dataSetMode = "Small"
  val featureEncoderType = "OneHot"
  val forceRebuild = true
}