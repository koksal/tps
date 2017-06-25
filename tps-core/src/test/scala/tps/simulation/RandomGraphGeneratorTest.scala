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

}
