package tps.simulation

import tps.Graphs.{Edge, UndirectedGraph}

/**
  * Generates a random graph of given size through a random walk on a given
  * [[UndirectedGraph]], starting from its sources.
  */
object RandomGraphGenerator {

  private val RANDOM_SEED = 131161511
  private val MAX_NODE_DEGREE = 3

  object CannotExtendException extends Exception

  def generateRandomGraph(sourceGraph: UndirectedGraph,
                          maxNodeLimit: Int): UndirectedGraph = {
    assert(sourceGraph.sources.size <= maxNodeLimit)

    val random = new scala.util.Random(RANDOM_SEED)
    var generatedGraph = UndirectedGraph(sourceGraph.sources, Set.empty,
      sourceGraph.sources)

    try {
      while (generatedGraph.V.size < maxNodeLimit) {
        // find a random incident edge to add while respecting max degree limit.
        val candidates = extensionCandidates(generatedGraph, sourceGraph)
        if (candidates.isEmpty) throw CannotExtendException

        val edgeToAdd = candidates.toIndexedSeq(random.nextInt(candidates.size))
        generatedGraph = UndirectedGraph(
          generatedGraph.V ++ Set(edgeToAdd.v1, edgeToAdd.v2),
          generatedGraph.E + edgeToAdd,
          generatedGraph.sources)
      }
      generatedGraph
    } catch {
      case CannotExtendException => generatedGraph
    }
  }

  private def extensionCandidates(graphToExtend: UndirectedGraph,
                                  sourceGraph: UndirectedGraph): Set[Edge] = {
    graphToExtend.V.flatMap { v =>
      sourceGraph.incidentEdges(v).filter { e =>
        // only take edges that do not exist and that won't exceed max indegree
        !graphToExtend.E.contains(e) &&
          graphToExtend.incidentEdges(e.v1).size < MAX_NODE_DEGREE &&
          graphToExtend.incidentEdges(e.v2).size < MAX_NODE_DEGREE
      }
    }
  }
}
