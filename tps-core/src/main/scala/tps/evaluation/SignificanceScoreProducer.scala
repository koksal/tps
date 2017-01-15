package tps.evaluation

import java.io.File

import tps.PeptideExpansion.PeptideProteinMap
import tps.PeptideProteinMappingParser
import tps.Profile
import tps.TabularData
import tps.TimeSeries
import tps.TimeSeriesParser
import tps.util.FileUtils

/**
  * Generates significance score and protein prize files based on fold change
  * thresholding from a time series file with a single replicate.
  */
object SignificanceScoreProducer {

  val SIGNIFICANT_SCORE = 0.0
  val NON_SIGNIFICANT_SCORE = 1.0

  def main(args: Array[String]): Unit = {
    val timeSeriesFile = new File(args(0))
    val mappingFile = new File(args(1))
    val threshold = args(2).toDouble
    val timeSeries = TimeSeriesParser.run(timeSeriesFile)
    val peptideProteinMap = PeptideProteinMappingParser.run(mappingFile)

    // compute fold changes
    val firstFCs = firstFoldChanges(timeSeries)
    val prevFCs = prevFoldChanges(timeSeries)

    // convert fold changes to scores
    val firstScores = scoresFromFoldChanges(firstFCs, threshold)
    val prevScores = scoresFromFoldChanges(prevFCs, threshold)

    // save scores to file
    saveScores(firstScores, firstScoreLabels(timeSeries), "first-scores.tsv")
    saveScores(prevScores, prevScoreLabels(timeSeries), "prev-scores.tsv")

    // take max fold change per peptide as prize
    val pepPrizes = peptidePrizes(firstFCs, prevFCs)

    // take max peptide prize for each protein
    val protPrizes = collapsePrizes(pepPrizes, peptideProteinMap)
    savePrizes(protPrizes)
  }

  private def saveScores(
    scores: Map[String, Seq[Double]], labels: Seq[String], fname: String
  ): Unit = {
    val tuples = scores map {
      case (id, vs) => id +: vs.map(_.toString)
    }
    val tsvString = TabularData(labels, tuples.toSeq).toTSVString()
    FileUtils.writeToFile(new File(fname), tsvString)
  }

  private def savePrizes(proteinPrizes: Map[String, Double]): Unit = {
    val tuples = proteinPrizes map {
      case (id, prize) => List(id, prize.toString)
    }
    val tsvString = TabularData(Nil, tuples.toSeq).toTSVString(
      printHeaders = false)
    FileUtils.writeToFile(new File("protein-prizes.tsv"), tsvString)
  }

  private def collapsePrizes(
    peptidePrizes: Map[String, Double], peptideProteinMap: PeptideProteinMap
  ): Map[String, Double] = {
    val proteinPeptideMap = reverseMap(peptideProteinMap)
    for ((prot, peps) <- proteinPeptideMap) yield {
      val maxPepPrize = peps.map(pep => peptidePrizes(pep)).max
      prot -> maxPepPrize
    }
  }

  private def firstFoldChanges(ts: TimeSeries): Map[String, Seq[Double]] = {
    ts.profiles.map{
      case Profile(id, vs) => {
        val baseline = vs.head.get
        val restValues = vs.tail
        val foldChanges = restValues map ( vOpt =>
          absLog2FoldChange(vOpt.get, baseline))
        id -> foldChanges
      }
    }.toMap
  }

  private def prevFoldChanges(ts: TimeSeries): Map[String, Seq[Double]] = {
    ts.profiles.map{
      case Profile(id, vs) => {
        val foldChanges = vs.zip(vs.tail).map {
          case (prevVal, currVal) => absLog2FoldChange(currVal.get, prevVal.get)
        }
        id -> foldChanges
      }
    }.toMap
  }

  private def peptidePrizes(
    firstFoldChanges: Map[String, Seq[Double]],
    prevFoldChanges: Map[String, Seq[Double]]
  ): Map[String, Double] = {
    assert(firstFoldChanges.keySet == prevFoldChanges.keySet)
    firstFoldChanges.keySet.map{ id =>
      id -> math.max(firstFoldChanges(id).max, prevFoldChanges(id).max)
    }.toMap
  }

  private def reverseMap(
    ppm: PeptideProteinMap
  ): Map[String, Set[String]] = {
    var proteinPeptideMap = Map[String, Set[String]]()
    for ((pep, prots) <- ppm) {
      for (prot <- prots) {
        proteinPeptideMap.get(prot) match {
          case Some(peps) => {
            proteinPeptideMap += prot -> (peps + pep)
          }
          case None => {
            proteinPeptideMap += prot -> Set(pep)
          }
        }
      }
    }
    proteinPeptideMap
  }

  private def firstScoreLabels(ts: TimeSeries): Seq[String] = {
    val baselineLabel = ts.labels.head
    val restLabels = ts.labels.tail
    val scoreLabels = restLabels map { l =>
      s"$l vs. $baselineLabel"
    }
    "id" +: scoreLabels
  }

  private def prevScoreLabels(ts: TimeSeries): Seq[String] = {
    val scoreLabels = ts.labels.zip(ts.labels.tail).map {
      case (prevLabel, currLabel) =>
        s"$currLabel vs. $prevLabel"
    }
    "id" +: scoreLabels
  }

  private def absLog2FoldChange(value: Double, baseline: Double): Double = {
    math.abs(math.log(value / baseline) / math.log(2))
  }

  private def scoresFromFoldChanges(
    peptideToFoldChanges: Map[String, Seq[Double]],
    threshold: Double
  ): Map[String, Seq[Double]] = {
    peptideToFoldChanges map {
      case (id, fcs) => {
        val scores = fcs map { fc =>
          if (fc >= threshold) {
            SIGNIFICANT_SCORE
          } else {
            NON_SIGNIFICANT_SCORE
          }
        }
        id -> scores
      }
    }
  }
}
