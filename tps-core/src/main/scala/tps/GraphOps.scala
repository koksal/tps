package tps

import Graphs._
import GraphSolutions._

import tps.util.LogUtils

object GraphOps {

  def reachable(
    origin: Set[Vertex], 
    graph: UndirectedGraph,
    edgeProperty: (Vertex, Vertex) => Boolean
  ): Set[Vertex] = {
    val ps = paths(origin, graph, edgeProperty, None)
    val reachableThroughPaths = ps map (_.end)

    reachableThroughPaths ++ origin
  }

  def reachableInOneStep(
    origin: Set[Vertex], 
    graph: UndirectedGraph, 
    edgeProperty: (Vertex, Vertex) => Boolean
  ): Set[Vertex] = {
    oneStepPaths(origin, graph, edgeProperty) map {
      case Path(Seq(Edge(v1, v2))) => v2
      case _ => LogUtils.terminate("Bad number of edges in one-step path.")
    }
  }

  def paths(
    origin: Set[Vertex], 
    graph: UndirectedGraph, 
    edgeProperty: (Vertex, Vertex) => Boolean, 
    maxPathLen: Option[Int]
  ): Set[Path] = {
    def pathLimitReached(i: Int) = maxPathLen match {
      case None => false
      case Some(l) => i >= l
    }

    var alreadySeen = origin
    var toProcess   = origin

    var computedPaths = Set[Path]()
    var stepCounter = 0

    while (!pathLimitReached(stepCounter) && !toProcess.isEmpty) {
      stepCounter += 1

      val nextStepPaths = oneStepPaths(toProcess, graph, edgeProperty)
      // val nextStepPathsToUnseen = nextStepPaths filter {
      //   p => !alreadySeen.contains(p.end)
      // }

      computedPaths ++= mergeNewSteps(computedPaths, nextStepPaths)
      toProcess = (nextStepPaths map (p => p.end)) -- alreadySeen
      alreadySeen ++= toProcess
    }

    computedPaths
  }

  def shortestDistances(
    origin: Set[Vertex],
    graph: UndirectedGraph
  ): Map[Vertex, Int] = {
    var alreadySeen = origin
    var toProcess   = origin

    var distance = 0
    var distances = Map[Vertex, Int]()

    for (v <- origin) {
      distances += v -> 0
    }

    while (!toProcess.isEmpty) {
      distance += 1
      val nextStep = toProcess.flatMap{ v =>
        graph.neighbors(v)
      }.filter{ v =>
        !alreadySeen.contains(v)
      }

      for (v <- nextStep) {
        distances += v -> distance
      }

      alreadySeen ++= nextStep
      toProcess = nextStep
    }

    distances
  }

  def mergeNewSteps(paths: Set[Path], newSteps: Set[Path]): Set[Path] = {
    var res = Set[Path]()
    for (ns @ Path(Seq(Edge(v1, v2))) <- newSteps) {
      val toExtend = paths filter (_.end == v1)
      val newPaths = toExtend map {
        case Path(es) => Path(es ++ ns.edges)
      }
      if (newPaths.isEmpty) {
        res += ns
      } else {
        res ++= newPaths
      }
    }
    res
  }

  def oneStepPaths(origin: Set[Vertex], graph: UndirectedGraph, 
      edgeProperty: (Vertex, Vertex) => Boolean): Set[Path] = {
    var paths = Set[Path]()

    for (o <- origin) {
      val dsts = graph.neighbors(o).filter{
        n => /*!origin.contains(n) &&*/ edgeProperty(o, n)
      }
      paths ++= dsts map (dst => Path(Seq(Edge(o, dst))))
    }

    paths
  }

  def addPath(g: UndirectedGraph, p: Path): UndirectedGraph = {
    val newV = g.V ++ p.vertices
    val newE = g.E ++ p.edges.toSet
    UndirectedGraph(newV, newE, g.sources)
  }

  def removeEdge(g: UndirectedGraph, e: Edge): UndirectedGraph = {
    val possiblyDisconnectedG = UndirectedGraph(g.V, g.E - e, g.sources)
    val connectedNodes = possiblyDisconnectedG.V filter {
      v => !possiblyDisconnectedG.neighbors(v).isEmpty
    }
    UndirectedGraph(connectedNodes, possiblyDisconnectedG.E, 
      g.sources intersect connectedNodes)

  }

  def union(g1: UndirectedGraph, g2: UndirectedGraph): UndirectedGraph = {
    val uV = g1.V ++ g2.V
    val uE = g1.E ++ g2.E
    val uSrc = g1.sources ++ g2.sources
    UndirectedGraph(uV, uE, uSrc)
  }

  def cliqueEdges(vs: Set[Vertex]): Set[Edge] = {
    if (vs.isEmpty)  {
      Set[Edge]()
    } else {
      val e = vs.head
      val rest = vs - e
      val eEdges = rest.map(Edge(e,_))
      eEdges ++ cliqueEdges(rest)
    }
  }

  def clique(vs: Set[Vertex]): UndirectedGraph = {
    UndirectedGraph(vs, cliqueEdges(vs), Set.empty)
  }

  def expandPaths(
    p: Path, 
    proteinsToPeptides: Map[String, Set[String]]
  ): Set[Path] =  p match {
    case Path(Seq(e)) => 
      expandEdges(e, proteinsToPeptides) map (newE => Path(Seq(newE)))
    case Path(Seq(e, es @ _*)) =>
      val restPaths = expandPaths(Path(es), proteinsToPeptides)
      val expandedFirstEdge = 
        expandEdges(e, proteinsToPeptides) map (newE => Path(Seq(newE)))
      expandedFirstEdge flatMap {
        case Path(Seq(fe @ Edge(v1, v2))) =>
          val matchingRestPaths = restPaths filter (_.start == v2)
          matchingRestPaths map {
            case Path(es) => Path(fe +: es)
          }
      }
  }

  def expandEdges(e: Edge, 
      protPeptideM: Map[String, Set[String]]): Set[Edge] = e match {
    case Edge(v1, v2) => {
      val ps1 = proteinToPeptideVertices(v1, protPeptideM)
      val ps2 = proteinToPeptideVertices(v2, protPeptideM)
      ps1 flatMap {
        p1 => ps2 map (p2 => Edge(p1, p2))
      }
    }
  }

  def proteinToPeptideVertices(v: Vertex, protPeptideM: Map[String, Set[String]]): 
      Set[Vertex] = protPeptideM.get(v.id) match {
    case Some(peps) => peps map (Vertex(_))
    case None => Set(v)
  }


  def fromAmbiguousSolution(s: AmbiguousGraphSolution): UndirectedGraph = {
    val es = s.keySet
    val vs = es.foldLeft(Set[Vertex]()){ case (acc, Edge(v1, v2)) => 
      acc ++ Set(v1, v2)
    }
    UndirectedGraph(vs, es, Set.empty)
  }

  def emptyGraph: UndirectedGraph = UndirectedGraph(Set.empty, Set.empty, Set.empty)
}
