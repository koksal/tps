package tps.evaluation.funchisq

import tps.Graphs.Edge
import tps.Graphs.EdgeDirection

object FunChisqGraphs {

  case class FunChisqScore(statistic: Double, pValue: Double)
  type FunChisqGraph = Map[Edge, (EdgeDirection, FunChisqScore)]

}
