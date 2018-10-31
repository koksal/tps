package tps.evaluation

import java.io.File

import tps.FileReporter
import tps.Profile
import tps.TimeSeriesParser
import tps.TimeSeriesScoresParser
import tps.printing.TimeSeriesPrinter
import tps.printing.TimeSeriesScoresPrinter
import tps.util.CollectionUtils

import scala.util.Random

object TimeSeriesPermutation {
  def main(args: Array[String]): Unit = {
    val timeSeries = TimeSeriesParser.run(new File(args(0)))
    val firstScores = TimeSeriesScoresParser.run(new File(args(1)))
    val prevScores = TimeSeriesScoresParser.run(new File(args(2)))
    val seed = args(3).toInt
    val outFolder = new File(args(4))

    val rand = new Random(seed)
    val permuteFun = permute(rand) _


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

    val permutedTimeSeries = timeSeries.copy(
      profiles = permutedProfiles.toSeq.sortBy(_.id))

    val reporter = new FileReporter(outFolder, Some("permuted"))
    reporter.output("time-series.tsv",
      TimeSeriesPrinter(permutedTimeSeries))
    reporter.output("first-scores.tsv",
      TimeSeriesScoresPrinter(permutedFirstScores))
    reporter.output("prev-scores.tsv",
      TimeSeriesScoresPrinter(permutedPrevScores))
  }

  private def permute(rand: Random)(
    profile: Profile, firstScores: Seq[Double], prevScores: Seq[Double]
  ): (Profile, Seq[Double], Seq[Double]) = {
    val n = profile.values.size - 1
    val permutation = rand.shuffle((0 until n).toList)

    // permute tail of profile values
    val permutedTailValues = CollectionUtils.permuteSeq(profile.values.tail,
      permutation)
    val permutedProfile = profile.copy(
      values = profile.values.head +: permutedTailValues)

    (
      permutedProfile,
      CollectionUtils.permuteSeq(firstScores, permutation),
      CollectionUtils.permuteSeq(prevScores, permutation)
    )
  }
}
