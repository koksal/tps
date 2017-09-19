package tps.simulation

import tps.Graphs
import tps.Graphs.{Edge, UndirectedGraph, Vertex}

/**
  * Generates a random graph of given size, according to a predetermined
  * likelihood of choosing between adding a new node and adding a new edge to
  * it.
  */
object RandomGraphGenerator {

  private val RANDOM_SEED = 131161511
  private val random = new scala.util.Random(RANDOM_SEED)

  private val NODE_CREATION_PROBABILITY = 0.5

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
