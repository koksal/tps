package tps.synthesis

import tps.GraphSolutions._

trait Solver {
  def summary(): SignedDirectedGraph
}
