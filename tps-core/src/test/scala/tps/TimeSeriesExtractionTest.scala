package tps
package test

import org.scalatest.FunSuite
import org.scalatest.Matchers

class TimeSeriesExtractionTest extends FunSuite with Matchers {
  test("read time series with missing values from file") {
    val url = getClass.getResource("/timeseries.tsv")
    val file = new java.io.File(url.getFile())
    val ts = TimeSeriesParser.run(file)

    val expectedTs = TimeSeries(
      labels = Seq("tp1", "tp2", "tp3"),
      profiles = Seq(
        Profile("A", Seq(Some(0), Some(1), Some(2))),
        Profile("B", Seq(Some(0), None, Some(2))),
        Profile("C", Seq(Some(0), Some(1), None))
      )
    )
    ts should equal (expectedTs)
  }
}
