package tps.synthesis

import Trees._

import tps.UndirectedGraphs._
import tps.Profile
import tps.TimeSeries

import tps.util.LogUtils

/* An intepretation encodes a view of the temporal traces. It takes the raw 
 * traces as input. It encapsulates a way to encode the constraints
 * imposed on the graph. */ 
trait Interpretation {

  val timeSeries: TimeSeries

  def vertexPattern(v: Vertex): Option[Profile] = {
    timeSeries.profileByID(v.id)
  }

  def makeSymbolic(): SymbolicInterpretation

}

trait SymbolicInterpretation {
  self =>

  val baseInterpretation: Interpretation

  def graph: UndirectedGraph

  def vertexPattern(v: Vertex): Option[Profile] = 
    baseInterpretation.vertexPattern(v)

  def solution(model: Map[Identifier, Expr]): InterpretationSolution

  def solutionFormula(sol: InterpretationSolution): Expr

  def consistency(): Expr
  def consistencyWithGraph(implicit sg: SymbolicGraph): Expr

}

trait InterpretationSolution
