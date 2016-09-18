package tps.simulation

import org.scalatest.FunSuite
import org.scalatest.Matchers
import tps.Graphs.Vertex
import tps.UndirectedGraphParser
import tps.TestResourceUtil.testFile

class RandomGraphGeneratorTest extends FunSuite with Matchers {

  test("completely random graph has one source and right number of edges") {
    val generated = RandomGraphGenerator.generateRandomGraph(5)

    // Check that there is only one source, and it is in the vertex set.
    generated.sources.size should equal (1)
    generated.V should contain (generated.sources.head)

    generated.E.size should equal (5)
  }

  test("random graph from existing graph contains source and has right size") {
    val sourceGraph = UndirectedGraphParser.run(testFile("network.tsv")).copy(
        sources = Set(Vertex("A")))
    val maxNodeLimit = 3
    val generated = RandomGraphGenerator.generateRandomGraphFromSourceGraph(
      sourceGraph,
      maxNodeLimit)

    generated.V.size should equal (3)
    generated.V should contain (Vertex("A"))
  }

}
