package tps.evaluation

import java.io.File

import tps.FileReporter
import tps.Profile
import tps.TabularData
import tps.TimeSeriesParser
import tps.TimeSeriesScoresParser
import tps.printing.TimeSeriesPrinter
import tps.printing.TimeSeriesScoresPrinter

import scala.util.Random

object TimeSeriesPermutation {
  def main(args: Array[String]): Unit = {
    val timeSeries = TimeSeriesParser.run(new File(args(0)))
    val firstScores = TimeSeriesScoresParser.run(new File(args(1)))
    val prevScores = TimeSeriesScoresParser.run(new File(args(2)))
    val seed = args(3).toInt

    val rand = new Random(seed)
    val permuteFun = permute(rand)


    var permutedProfiles = Set[Profile]()
    var permutedFirstScores = Map[String, Seq[Double]]()
    var permutedPrevScores = Map[String, Seq[Double]]()

    for (profile <- timeSeries.profiles) {
      val (permutedProfile, permutedFirst, permutedPrev) = permuteFun(
        profile, firstScores(profile.id), prevScores(profile.id))
      permutedProfiles += permutedProfile
      permutedFirstScores += profile.id -> permutedFirst
      permutedPrevScores += profile.id -> permutedPrev
    }

    val permutedTimeSeries = timeSeries.copy(profiles = permutedProfiles.toSeq)

    val reporter = new FileReporter(new File("."), None)
    reporter.output("permuted-time-series.tsv",
      TimeSeriesPrinter(permutedTimeSeries))
    reporter.output("permuted-first-scores.tsv",
      TimeSeriesScoresPrinter(firstScores))
    reporter.output("permuted-prev-scores.tsv",
      TimeSeriesScoresPrinter(prevScores))
  }

  private def permute(rand: Random)(
    profile: Profile, firstScores: Seq[Double], prevScores: Seq[Double]
  ): (Profile, Seq[Double], Seq[Double]) = {
    val n = profile.values.size - 1
    val permutation = rand.shuffle(0 until n).toSeq

    // permute tail of profile values
    val permutedTailValues = permuteSeq(profile.values.tail, permutation)
    val permutedProfile = profile.copy(
      values = profile.values.head +: permutedTailValues)

    (
      permutedProfile,
      permuteSeq(firstScores, permutation),
      permuteSeq(prevScores, permutation)
    )
  }

  private def permuteSeq[T](xs: Seq[T], permutation: Seq[Int]): Seq[T] = {
    permutation map (i => xs(i))
  }
}
