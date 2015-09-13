package tps

import UndirectedGraphs._

import tps.util.FileUtils
import tps.util.LogUtils
import tps.util.MathUtils

import java.io.File

object GraphSolutions {

  type AmbiguousGraphSolution = Map[Edge, Set[EdgeSolution]]

  sealed trait EdgeSolution
  case class ActiveEdge(direction: EdgeDirection, sign: EdgeSign) extends EdgeSolution
  case object InactiveEdge extends EdgeSolution

  sealed trait EdgeDirection
  case object Forward   extends EdgeDirection
  case object Backward  extends EdgeDirection

  sealed trait EdgeSign
  case object Activating  extends EdgeSign
  case object Inhibiting  extends EdgeSign

  def activeSolVertices(s: AmbiguousGraphSolution): Set[Vertex] = {
    s.flatMap{ case (Edge(v1, v2), ess) => 
      if (!noDirection(ess)) Set(v1, v2) else Set[Vertex]()
    }.toSet
  }

  def aggregate(
    s1: AmbiguousGraphSolution, 
    s2: AmbiguousGraphSolution
  ): AmbiguousGraphSolution = {
    var es1 = s1.keySet
    var es2 = s2.keySet

    val aggregate = (es1 ++ es2).map{ e =>
      var newEdgeSol = Set[EdgeSolution]()
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

  def intersectCommonEdges(
    s1: AmbiguousGraphSolution,
    s2: AmbiguousGraphSolution
  ): AmbiguousGraphSolution = {
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

  def ambigSolFromFile(f: File): AmbiguousGraphSolution = {
    var sol: AmbiguousGraphSolution = Map.empty
    for (l <- FileUtils.lines(f).tail) {
      val List(src, tgt, lra, lri, rla, rli) = l.split("\t").toList
      val e = Edge(Vertex(src), Vertex(tgt))
      var ess = Set[EdgeSolution]()
      if (lra == "true") ess += ActiveEdge(Forward, Activating)
      if (lri == "true") ess += ActiveEdge(Forward, Inhibiting)
      if (rla == "true") ess += ActiveEdge(Backward, Activating)
      if (rli == "true") ess += ActiveEdge(Backward, Inhibiting)
      sol += e -> ess
    }
    sol
  }

  def printPrediction(e: Edge, ps: AmbiguousGraphSolution): String = e match {
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

  def printPrediction(e: Edge, ess: Set[EdgeSolution]): String = {
    val onlyActive = ess filter (_ != InactiveEdge)
    val toDisplay = if (onlyActive.isEmpty) ess else onlyActive
    toDisplay.map(edgeSolString(e, _)).mkString(" / ")
  }

  private def edgeSolString(e: Edge, es: EdgeSolution) = e match {
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

  def reverse(es: EdgeSolution): EdgeSolution = es match {
    case InactiveEdge => es
    case ActiveEdge(d, s) => ActiveEdge(reverse(d), s)
  }

  def reverse(d: EdgeDirection): EdgeDirection = d match {
    case Forward => Backward
    case Backward => Forward
  }

  def inDegree(v: Vertex, sol: AmbiguousGraphSolution): Int = {
    val relevant = sol filter {
      case (Edge(v1, v2), es) => 
        v == v1 && canBeBackward(es) ||
        v == v2 && canBeForward(es)
    }
    relevant.size
  }

  def activeEdgeRatio(sol: AmbiguousGraphSolution): Double = {
    val nbEdges = sol.size
    val activeEdges = sol.filter {
      case (e, es) => !noDirection(es)
    }
    MathUtils.roundTo(activeEdges.size.toDouble / nbEdges, 2)
  }

  def unidirEdgeRatio(sol: AmbiguousGraphSolution): Double = {
    val nbEdges = sol.size
    MathUtils.roundTo(nbUnidirEdges(sol).toDouble / nbEdges, 2)
  }

  def activeEdges(sol: AmbiguousGraphSolution): Set[Edge] = {
    val es = sol collect { 
      case (e, es) if !noDirection(es) => e
    }
    es.toSet
  }

  def noDirection(es: Set[EdgeSolution]): Boolean = {
    es == Set(InactiveEdge)
  }

  def canBeInactive(es: Set[EdgeSolution]): Boolean = {
    es contains InactiveEdge
  }

  def oneActiveDirection(es: Set[EdgeSolution]): Boolean = {
    onlyForward(es) || onlyBackward(es)
  }

  def onlyBackward(es: Set[EdgeSolution]): Boolean = {
    canBeBackward(es) && !canBeForward(es)
  }

  def onlyForward(es: Set[EdgeSolution]): Boolean = {
    canBeForward(es) && !canBeBackward(es)
  }

  def canBeForward(es: Set[EdgeSolution]): Boolean = {
    es exists {
      case ActiveEdge(Forward, _) => true
      case _ => false
    }
  }

  def canBeBackward(es: Set[EdgeSolution]): Boolean = {
    es exists {
      case ActiveEdge(Backward, _) => true
      case _ => false
    }
  }

  def onlyActivating(es: Set[EdgeSolution]): Boolean = {
    canBeActivating(es) && !canBeInhibiting(es)
  }

  def onlyInhibiting(es: Set[EdgeSolution]): Boolean = {
    canBeInhibiting(es) && !canBeActivating(es)
  }

  def canBeActivating(es: Set[EdgeSolution]): Boolean = {
    es exists {
      case ActiveEdge(_, Activating) => true
      case _ => false
    }
  }

  def canBeInhibiting(es: Set[EdgeSolution]): Boolean = {
    es exists {
      case ActiveEdge(_, Inhibiting) => true
      case _ => false
    }
  }

  def onlyForward(e: Edge, sol: AmbiguousGraphSolution): Boolean = {
    testSolution(e, sol, onlyForward, onlyBackward)
  }

  def nonambiguous(es: Set[EdgeSolution]): Boolean = {
    !noDirection(es) && !ambiguous(es)
  }

  def ambiguous(es: Set[EdgeSolution]): Boolean = {
    ambiguousDirection(es) || ambiguousSign(es)
  }

  def ambiguousSign(es: Set[EdgeSolution]): Boolean = {
    canBeActivating(es) && canBeInhibiting(es)
  }

  def ambiguousDirection(es: Set[EdgeSolution]): Boolean = {
    canBeForward(es) && canBeBackward(es)
  }

  def ambiguousDirection(e: Edge, sol: AmbiguousGraphSolution): Boolean = {
    testSolution(e, sol, ambiguousDirection, ambiguousDirection)
  }

  def canBeForward(e: Edge, sol: AmbiguousGraphSolution): Boolean = {
    testSolution(e, sol, 
      (es: Set[EdgeSolution]) => canBeForward(es),
      (es: Set[EdgeSolution]) => canBeBackward(es))
  }

  def testSolution(e: Edge, sol: AmbiguousGraphSolution, 
      forwardTest: (Set[EdgeSolution]) => Boolean,
      backwardTest: (Set[EdgeSolution]) => Boolean): Boolean = e match {
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

  def nbUnidirEdges(sol: AmbiguousGraphSolution): Int = {
    unidirEdges(sol).size
  }

  def nbNonAmbigEdges(sol: AmbiguousGraphSolution): Int = {
    nonAmbigEdges(sol).size
  }

  def ambigEdges(sol: AmbiguousGraphSolution): Iterable[Edge] = {
    sol collect {
      case (e, es) if ambiguous(es) => e
    }
  }

  def nonAmbigEdges(sol: AmbiguousGraphSolution): Iterable[Edge] = {
    sol collect { 
      case (e, es) if nonambiguous(es) => e
    }
  }

  def unidirEdges(sol: AmbiguousGraphSolution): Iterable[Edge] = {
    sol collect {
      case (e, es) if oneActiveDirection(es) => e
    }
  }

  def allIncidentEdgesAmb(g: UndirectedGraph, v: Vertex, s: AmbiguousGraphSolution) = {
    val incident = g.incidentEdges(v)
    incident.forall{
      ambiguousDirection(_, s)
    }
  }

}
