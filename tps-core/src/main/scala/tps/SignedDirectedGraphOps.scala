package tps

import Graphs._

import tps.util.FileUtils
import tps.util.LogUtils
import tps.util.MathUtils

import java.io.File

object SignedDirectedGraphOps {

  def activeSolVertices(s: SignedDirectedGraph): Set[Vertex] = {
    s.flatMap{ case (Edge(v1, v2), ess) => 
      if (!ess.isEmpty) Set(v1, v2) else Set[Vertex]()
    }.toSet
  }

  def aggregate(
    s1: SignedDirectedGraph, 
    s2: SignedDirectedGraph
  ): SignedDirectedGraph = {
    var es1 = s1.keySet
    var es2 = s2.keySet

    val aggregate = (es1 ++ es2).map{ e =>
      var newEdgeSol = Set[SignedDirectedEdgeLabel]()
      for (s <- Set(s1, s2)) {
        s.get(e) match {
          case Some(ess) => newEdgeSol ++= ess
          case None =>
        }
      }
      e -> newEdgeSol
    }.toMap

    aggregate
  }

  def unifyByIntersectingCommonEdges(
    s1: SignedDirectedGraph,
    s2: SignedDirectedGraph
  ): SignedDirectedGraph = {
    var es1 = s1.keySet
    var es2 = s2.keySet

    val intersection = (es1 ++ es2).map{ e =>
      (s1.get(e), s2.get(e)) match {
        case (Some(ess1), Some(ess2)) => e -> (ess1 intersect ess2)
        case (Some(ess1), None) => e -> ess1
        case (None, Some(ess2)) => e -> ess2
        case (None, None) => throw new Exception("Cannot happen.")
      }
    }.toMap

    intersection
  }

  def ambigSolFromFile(f: File): SignedDirectedGraph = {
    var sol: SignedDirectedGraph = Map.empty
    for (l <- FileUtils.lines(f).tail) {
      val List(src, tgt, lra, lri, rla, rli) = l.split("\t").toList
      val e = Edge(Vertex(src), Vertex(tgt))
      var ess = Set[SignedDirectedEdgeLabel]()
      if (lra == "true") ess += ActiveEdge(Forward, Activating)
      if (lri == "true") ess += ActiveEdge(Forward, Inhibiting)
      if (rla == "true") ess += ActiveEdge(Backward, Activating)
      if (rli == "true") ess += ActiveEdge(Backward, Inhibiting)
      sol += e -> ess
    }
    sol
  }

  def printPrediction(e: Edge, ps: SignedDirectedGraph): String = e match {
    case Edge(Vertex(id1), Vertex(id2)) => {
      ps.get(e) match {
        case None => {
          val edgeInOtherDir = Edge(Vertex(id2), Vertex(id1))
          ps.get(edgeInOtherDir) match {
            case Some(ess) => printPrediction(edgeInOtherDir, ess)
            case None => s"No prediction between $id1 and $id2"
          }
        }
        case Some(ess) => printPrediction(e, ess)
      }
    }
  }

  def printPrediction(e: Edge, ess: Set[SignedDirectedEdgeLabel]): String = {
    ess.map(edgeSolString(e, _)).mkString(" / ")
  }

  private def edgeSolString(e: Edge, es: SignedDirectedEdgeLabel) = e match {
    case Edge(Vertex(id1), Vertex(id2)) => es match {
      case InactiveEdge =>
        s"$id1 -- $id2"
      case ActiveEdge(Forward, Activating) =>
        s"$id1 → $id2"
      case ActiveEdge(Forward, Inhibiting) =>
        s"$id1 ⊣ $id2"
      case ActiveEdge(Backward, Activating) =>
        s"$id2 → $id1"
      case ActiveEdge(Backward, Inhibiting) =>
        s"$id2 ⊣ $id1"
    }
  }

  def reverse(es: SignedDirectedEdgeLabel): SignedDirectedEdgeLabel = es match {
    case InactiveEdge => es
    case ActiveEdge(d, s) => ActiveEdge(reverse(d), s)
  }

  def reverse(d: EdgeDirection): EdgeDirection = d match {
    case Forward => Backward
    case Backward => Forward
  }

  def inDegree(v: Vertex, sol: SignedDirectedGraph): Int = {
    val relevant = sol filter {
      case (Edge(v1, v2), es) => 
        v == v1 && canBeBackward(es) ||
        v == v2 && canBeForward(es)
    }
    relevant.size
  }

  def oneActiveDirection(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    onlyForward(es) || onlyBackward(es)
  }

  def onlyBackward(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    canBeBackward(es) && !canBeForward(es)
  }

  def onlyForward(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    canBeForward(es) && !canBeBackward(es)
  }

  def canBeForward(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    es exists {
      case ActiveEdge(Forward, _) => true
      case _ => false
    }
  }

  def canBeBackward(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    es exists {
      case ActiveEdge(Backward, _) => true
      case _ => false
    }
  }

  def onlyActivating(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    canBeActivating(es) && !canBeInhibiting(es)
  }

  def onlyInhibiting(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    canBeInhibiting(es) && !canBeActivating(es)
  }

  def canBeActivating(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    es exists {
      case ActiveEdge(_, Activating) => true
      case _ => false
    }
  }

  def canBeInhibiting(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    es exists {
      case ActiveEdge(_, Inhibiting) => true
      case _ => false
    }
  }

  def onlyForward(e: Edge, sol: SignedDirectedGraph): Boolean = {
    testSolution(e, sol, onlyForward, onlyBackward)
  }

  def ambiguous(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    ambiguousDirection(es) || ambiguousSign(es)
  }

  def ambiguousSign(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    canBeActivating(es) && canBeInhibiting(es)
  }

  def ambiguousDirection(es: Set[SignedDirectedEdgeLabel]): Boolean = {
    canBeForward(es) && canBeBackward(es)
  }

  def ambiguousDirection(e: Edge, sol: SignedDirectedGraph): Boolean = {
    testSolution(e, sol, ambiguousDirection, ambiguousDirection)
  }

  def canBeForward(e: Edge, sol: SignedDirectedGraph): Boolean = {
    testSolution(e, sol, 
      (es: Set[SignedDirectedEdgeLabel]) => canBeForward(es),
      (es: Set[SignedDirectedEdgeLabel]) => canBeBackward(es))
  }

  def testSolution(e: Edge, sol: SignedDirectedGraph, 
      forwardTest: (Set[SignedDirectedEdgeLabel]) => Boolean,
      backwardTest: (Set[SignedDirectedEdgeLabel]) => Boolean): Boolean = e match {
    case Edge(v1, v2) => {
      sol.get(Edge(v1, v2)) match {
        case Some(ds) => forwardTest(ds)
        case None => sol.get(Edge(v2, v1)) match {
          case Some(ds) => backwardTest(ds)
          case None => LogUtils.terminate("No solution for edge " + Edge(v1, v2))
        }
      }
    }
  }

  def ambigEdges(sol: SignedDirectedGraph): Iterable[Edge] = {
    sol collect {
      case (e, es) if ambiguous(es) => e
    }
  }

  def unidirEdges(sol: SignedDirectedGraph): Iterable[Edge] = {
    sol collect {
      case (e, es) if oneActiveDirection(es) => e
    }
  }

  def allIncidentEdgesAmb(g: UndirectedGraph, v: Vertex, s: SignedDirectedGraph) = {
    val incident = g.incidentEdges(v)
    incident.forall{
      ambiguousDirection(_, s)
    }
  }

}
