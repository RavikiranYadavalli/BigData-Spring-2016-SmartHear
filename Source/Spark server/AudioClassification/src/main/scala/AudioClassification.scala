/**
  *@author : Ravi kiran Yadavalli
  */

import java.io.{PrintStream, File}
import java.net.ServerSocket
import javax.sound.sampled.AudioInputStream
import jAudioFeatureExtractor.AudioFeatures._
import jAudioFeatureExtractor.jAudioTools.AudioSamples
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.apache.spark.{SparkConf, SparkContext}
import scala.io.BufferedSource

/**
  *
  */
object AudioFeature extends Enumeration {
  type AudioFeature = Value
  val Spectral_Centroid, Spectral_Rolloff_Point, Spectral_Flux, Compactness, Spectral_Variability, Root_Mean_Square, Fration_of_Low_Energy_Windows, Zero_Crossings, Strongest_Beat, Beat_Sum, MFCC, ConstantQ, LPC, Method_of_Moments, Peak_Detection, Area_Method_of_MFCCs = Value

}

object AudioClassification {

  val TRAINING_PATH = "data/training/*"
  val TESTING_PATH = "data/testing/*"

  val AUDIO_CATEGORIES_HOME = List("doorbell", "doorknock","siren", "dogbark","telephone")
  val AUDIO_CATEGORIES_OUTDOOR = List("traffic","car","train","ambulance","bike")
  val AUDIO_CATEGORIES_CLASS = List("man","woman","group","siren")
    //List("bike","blender","boilingWater","cough","doorBell","dyerMachine","flushing","fondoSilencio","fondoTranquilo","kick","knockingDoor","shower","snare","stew","tel","toothBrushing","vaccum","washingDishes","washingMachine")

  def main(args: Array[String]) {
    System.setProperty("hadoop.home.dir", "F:\\winutils")
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName("SparkDecisionTree").set("spark.driver.memory", "4g")
    val sc = new SparkContext(sparkConf)
    val server = new ServerSocket(9999)
    var flag = false

//
//val features1 = AudioFeatureExtraction("F:/Downloads/CS5542-Tutorial10-SourceCode/CS5542-Tutorial10-SourceCode/AudioClassification/data/training/bike/hund.wav")
  //      println("Features from server are:" + features1)

//        var output = ""
//        println("server Enter")
//        while (true) {
//          val s = server.accept()
//          if (flag) {
//            val out = new PrintStream(s.getOutputStream())
//            println(output)
//            out.println(output)
//            out.flush()
//            flag = false
//            s.close()
//
//          }
//          else {
//            println("server UP")
//            val in = new BufferedSource(s.getInputStream()).getLines()
//            val input = in.mkString(" ")
//            //        val  str = in.toString()
//            //        println("string is: " +" " + input +str )
//            if (input.contains("ANALYZE")) {
//
//              val splitInput = input.split("ANALYZE :")
//              val context= (splitInput.mkString ).split("-")
//              println("context is: " + " " + context(0) )
//            //  val features1 = AudioFeatureExtraction("F:/Downloads/CS5542-Tutorial10-SourceCode/CS5542-Tutorial10-SourceCode/AudioClassification/data/training/bike/hund.wav")
//              //println("Features from server are:" + features1)
//              val featureBuilder = StringBuilder.newBuilder
//              context(1).map(feature => featureBuilder.append(feature))
//              println("featurebuilder is: " + " " + featureBuilder.toString())
//              val numClasses = 10
//              val categoricalFeaturesInfo = Map[Int, Int]()
//              val numTrees = 10 // Use more in practice.
//              val featureSubsetStrategy = "auto" // Let the algorithm choose.
//              val impurity = "gini"
//              val maxDepth = 4
//              val maxBins = 32
//             //  val model = RandomForest.trainClassifier(X_train, numClasses, categoricalFeaturesInfo,
//              // numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
//
//             val sameModel = DecisionTreeModel.load(sc, "myDecisionTreeClassificationModelWithFeatures")
//              //          val labelAndPreds = X_test.map { point =>
//              //            val prediction = sameModel.predict(point.features)
//              //            (point.label, prediction)
//              //          }
//              // println("what is this"+(featureBuilder.toString().split(';').map(_.toDouble)))
//              val labelAndPreds = sameModel.predict(Vectors.dense(featureBuilder.toString().split(';').map(_.toDouble)))
//              println("prediction " + labelAndPreds)
//              println("Learned classification tree model:\n" + sameModel.toDebugString)
//              output = AUDIO_CATEGORIES(labelAndPreds.toInt)
//              flag = true
//              s.close()
//            }
//          }
//        }
   //  training data
    val train = sc.textFile("data/class_Context_TrainingData.txt")
      val X_train= train.map ( line =>{

          val parts = line.split(':')
          println(AUDIO_CATEGORIES_CLASS.indexOf(parts(0)).toDouble, Vectors.dense(parts(1).split(';').map(_.toDouble)))
          LabeledPoint(AUDIO_CATEGORIES_CLASS.indexOf(parts(0)).toDouble, Vectors.dense(parts(1).split(';').map(_.toDouble)))

    })


    val test = sc.textFile("data/class_Context_TrainingData.txt")
    val X_test= train.map ( line =>{

        val parts = line.split(':')
        println(AUDIO_CATEGORIES_CLASS.indexOf(parts(0)).toDouble, Vectors.dense(parts(1).split(';').map(_.toDouble)))
        LabeledPoint(AUDIO_CATEGORIES_CLASS.indexOf(parts(0)).toDouble, Vectors.dense(parts(1).split(';').map(_.toDouble)))

    })

    val numClasses = 4
    val categoricalFeaturesInfo = Map[Int, Int]()
    val impurity = "gini"
    val maxDepth = 5
    val maxBins = 32

    val model = DecisionTree.trainClassifier(X_train, numClasses, categoricalFeaturesInfo,
      impurity, maxDepth, maxBins)



    val labelAndPreds = X_test.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    labelAndPreds.foreach(f=>{
      println("prediction" +f._1 + " Actual Label" + f._2)

    })
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / X_test.count()
    println("Test Error = " + testErr)
    println("Learned classification forest model:\n" + model.toDebugString)
    val  accuracy = 1.0 * labelAndPreds.filter(x => x._1 == x._2).count() / test.count()



    println("Prediction and label" + labelAndPreds)
    println("Accuracy : " + accuracy)

    val metrics = new MulticlassMetrics(labelAndPreds)
    //println("Confusion Matrix \n \n : "+metrics.confusionMatrix)
    println("Fmeasure is:"+metrics.fMeasure + "Precision is:" +metrics.precision)
    // Save and load model
    model.save(sc, "ContextDecisionTreeClassificationModel_class")
    //val sameModel = DecisionTreeModel.load(sc, "myDecisionTreeClassificationModelWithFeatures")
   // sameModel.save(sc, "myRandomForestClassificationModel1")


  }
  def AudioFeatureExtraction(path: String): String = {

    val audio: AudioSamples = new AudioSamples(new File(path), path, false)

    val f: Array[Double] = feature(audio, AudioFeature.Zero_Crossings)
    val meanZCR = calculateMean(f);
    //val f1: Array[Double] = feature(audio, AudioFeature.Spectral_Flux)
    val f1: Array[Double] = feature(audio, AudioFeature.MFCC)
    val meanMFCC = calculateMean(f1)
    val f2: Array[Double] = feature(audio, AudioFeature.Spectral_Rolloff_Point)
    val meanSpectralRollOff = calculateMean(f2)
    val f3: Array[Double] = feature(audio, AudioFeature.Fration_of_Low_Energy_Windows)
    val meanLowEnergyWindows = calculateMean(f3)
    val f4: Array[Double] = feature(audio, AudioFeature.Peak_Detection)
    val meanPeakValue = calculateMean(f4)
    // val f6: Array[Double] = feature(audio, AudioFeature.LPC)
    val f5: Array[Double] = feature(audio, AudioFeature.Root_Mean_Square)
    val meanRMS = calculateMean(f5)
    val f6: Array[Double] = feature(audio, AudioFeature.Compactness)
    val meanCompactness = calculateMean(f6)
    val str = meanZCR + ";" + meanMFCC + ";" + meanSpectralRollOff + ";" + meanPeakValue + ";" + meanRMS + ";" + meanCompactness + ";"
    //val str = f(0) + ";" + f1(0) + ";" + f2(0) + ";" + f3(0) + ";" + f4(0) + ";"  + f5(0) + ";" + f6(0) + ";"
    println("Features extracted are : " + str)


    str
  }

  @throws(classOf[Exception])
  def feature(audio: AudioSamples, i: AudioFeature.Value): Array[Double] = {
    var featureExt: FeatureExtractor = null
    val audioInputStream: AudioInputStream = audio.getAudioInputStreamMixedDown
    val samples: Array[Array[Double]] = audio.getSampleWindowsMixedDown(2825)
    val featureMeanSampleArray  = new Array[Double](1000)
    val sampleRate: Double = 44100
    val otherFeatures = Array.ofDim[Double](1000, 1000)
    var windowSample: Array[Array[Double]] = null
    val sampleRate1 = audio.getSamplingRateAsDouble
    println("sampling rate is:" +sampleRate1)
    println("samples length is:"+ samples.length)
    println("Frame length is is:"+ audioInputStream.getFrameLength)
    for(index<-0 until samples.length){
      i match {
        case AudioFeature.Spectral_Centroid =>
          featureExt = new PowerSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureExt = new SpectralCentroid

          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Spectral_Rolloff_Point =>
          featureExt = new PowerSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new SpectralRolloffPoint
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Compactness =>
          featureExt = new MagnitudeSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new Compactness
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Spectral_Variability =>
          featureExt = new MagnitudeSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new SpectralVariability
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Root_Mean_Square =>
          featureExt = new RMS
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Fration_of_Low_Energy_Windows =>
          featureExt = new RMS
          windowSample = audio.getSampleWindowsMixedDown(5)
          for (j <- 0 to 100)
            otherFeatures(j) = featureExt.extractFeature(windowSample(j), sampleRate, null)
          featureExt = new FractionOfLowEnergyWindows
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Zero_Crossings =>
          featureExt = new ZeroCrossings
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

        case AudioFeature.Strongest_Beat =>
          featureExt = new BeatHistogram
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new BeatHistogramLabels
          otherFeatures(1) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new StrongestBeat
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)
        case AudioFeature.Beat_Sum =>
          featureExt = new BeatHistogram
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new BeatSum
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)
        case AudioFeature.MFCC =>
          featureExt = new MagnitudeSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new MFCC
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample) + 100
        case AudioFeature.ConstantQ =>
          featureExt = new ConstantQ
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)
        case AudioFeature.LPC =>
          featureExt = new LPC
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)
        case AudioFeature.Method_of_Moments =>
          featureExt = new MagnitudeSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new Moments
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)
        case AudioFeature.Peak_Detection =>
          featureExt = new MagnitudeSpectrum
          otherFeatures(0) = featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          featureExt = new PeakFinder
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)
        case AudioFeature.Area_Method_of_MFCCs =>
          featureExt = new MagnitudeSpectrum
          windowSample = audio.getSampleWindowsMixedDown(100)
          for (j <- 0 to 100)
            otherFeatures(j) = featureExt.extractFeature(windowSample(j), sampleRate, null)
          featureExt = new AreaMoments
          featureExt.extractFeature(samples(0), sampleRate, otherFeatures)
          val windowFeatureSample = featureExt.extractFeature(samples(index), sampleRate, otherFeatures)
          featureMeanSampleArray(index) = calculateMean(windowFeatureSample)

      }

    }
    featureMeanSampleArray
  }

  @throws(classOf[Exception])
  def calculateMean(sample: Array[Double]): Double = {
    var meanValue : Double =0
    for(i<-0 until sample.length)
    {
      meanValue += sample(i)
    }
    if (sample.length != 0 )
    meanValue = meanValue/sample.length
    meanValue
  }
}
