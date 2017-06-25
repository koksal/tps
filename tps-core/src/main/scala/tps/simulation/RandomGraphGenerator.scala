package tps.simulation

import tps.Graphs
import tps.Graphs.{Edge, UndirectedGraph, Vertex}

/**
  * Generates a random graph of given size through a random walk on a given
  * [[UndirectedGraph]], starting from its sources.
  */
object RandomGraphGenerator {

  private val RANDOM_SEED = 131161511
  private val random = new scala.util.Random(RANDOM_SEED)

  // for creating graphs from a source graph
  private val MAX_NODE_DEGREE = 3

  // for creating purely random graphs
  private val NODE_CREATION_PROBABILITY = 0.5

  object CannotExtendException extends Exception

  def generateRandomGraph(nbEdges: Int): UndirectedGraph = {
    val src = Vertex("src")
    var V = Set[Vertex](src)
    var E = Set[Edge]()

    var i = 0
    while (E.size < nbEdges) {
      val srcVertex = V.toIndexedSeq(random.nextInt(V.size))
      val tgtVertex = if (random.nextDouble() < NODE_CREATION_PROBABILITY) {
        // add a new node
        i += 1
        Vertex(s"v_$i")
      } else {
        // add an edge to an existing node
        V.toIndexedSeq(random.nextInt(V.size))
      }
      // do not add self edges
      if (srcVertex != tgtVertex) {
        V += tgtVertex
        E += Graphs.lexicographicEdge(srcVertex, tgtVertex)
      }
    }

    UndirectedGraph(V, E, Set(src))
  }

}
