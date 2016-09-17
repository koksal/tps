package tps.simulation

import org.scalatest.FunSuite
import org.scalatest.Matchers
import tps.Graphs.Vertex
import tps.UndirectedGraphParser
import tps.TestResourceUtil.testFile

class RandomGraphGeneratorTest extends FunSuite with Matchers {

  test("random graph contains source and has right size") {
    val sourceGraph = UndirectedGraphParser.run(testFile("network.tsv")).copy(
        sources = Set(Vertex("A")))
    val maxNodeLimit = 3
    val generated = RandomGraphGenerator.generateRandomGraph(
      sourceGraph,
      maxNodeLimit)

    generated.V.size should equal (3)
    generated.V should contain (Vertex("A"))
  }

}
