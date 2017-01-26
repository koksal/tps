package tps.evaluation

import java.io.File

import tps.Graphs.SignedDirectedGraph
import tps.util.MathUtils
import tps.{SignedDirectedGraphOps, SignedDirectedGraphParser}

object DirectedNetworkComparison {
  def main(args: Array[String]): Unit = {
    // first argument: reference signed directed network
    // rest of arguments: collection of signed directed networks to compare to
    val allNetworkFiles = args map (arg => new File(arg))
    val allNetworks = allNetworkFiles map (
      f => SignedDirectedGraphParser.run(f))

    val firstNetwork = allNetworks.head
    val restNetworks = allNetworks.tail
    println(s"Comparing first network to ${restNetworks.size} networks.")

    val results = restNetworks map { n =>
      compareSignedDirectedNetworks(firstNetwork, n)
    }

    val aggregateResult = aggregateResults(results)
    println(aggregateResult)
  }

  case class SignedDirectedGraphComparisonResult(
    nbDirectedEdges1: Double,
    nbDirectedEdges2: Double,
    nbCommonDirectedEdges: Double,
    nbDirectedEdgesInAgreement: Double,
    nbDirectedEdgesInConflict: Double,
    nbDirectedEdgesOnlyIn1: Double,
    nbDirectedEdgesOnlyIn2: Double
  )

  private def compareSignedDirectedNetworks(
    n1: SignedDirectedGraph,
    n2: SignedDirectedGraph
  ): SignedDirectedGraphComparisonResult = {
    val directed1 = n1 filter {
      case (e, ess) => SignedDirectedGraphOps.oneActiveDirection(ess)
    }
    val directed2 = n2 filter {
      case (e, ess) => SignedDirectedGraphOps.oneActiveDirection(ess)
    }

    val commonDirectedE = directed1.keySet.intersect(directed2.keySet)
    val directedEdgesInAgreement = commonDirectedE filter { e =>
      val ess1 = n1(e)
      val ess2 = n2(e)

      (SignedDirectedGraphOps.onlyForward(ess1) &&
        SignedDirectedGraphOps.onlyForward(ess2)) ||
      (SignedDirectedGraphOps.onlyBackward(ess1) &&
        SignedDirectedGraphOps.onlyBackward(ess2))
    }

    val directedEdgesInConflict = commonDirectedE filter { e =>
      val ess1 = n1(e)
      val ess2 = n2(e)

      (SignedDirectedGraphOps.onlyForward(ess1) &&
        SignedDirectedGraphOps.onlyBackward(ess2)) ||
      (SignedDirectedGraphOps.onlyBackward(ess1) &&
        SignedDirectedGraphOps.onlyForward(ess2))
    }

    val directedOnlyIn1 = directed1.keySet -- directed2.keySet
    val directedOnlyIn2 = directed2.keySet -- directed1.keySet

    SignedDirectedGraphComparisonResult(
      directed1.size,
      directed2.size,
      commonDirectedE.size,
      directedEdgesInAgreement.size,
      directedEdgesInConflict.size,
      directedOnlyIn1.size,
      directedOnlyIn2.size
    )
  }

  private def aggregateResults(
    results: Iterable[SignedDirectedGraphComparisonResult]
  ): SignedDirectedGraphComparisonResult = {
    SignedDirectedGraphComparisonResult(
      MathUtils.median(results.map(_.nbDirectedEdges1)),
      MathUtils.median(results.map(_.nbDirectedEdges2)),
      MathUtils.median(results.map(_.nbCommonDirectedEdges)),
      MathUtils.median(results.map(_.nbDirectedEdgesInAgreement)),
      MathUtils.median(results.map(_.nbDirectedEdgesInConflict)),
      MathUtils.median(results.map(_.nbDirectedEdgesOnlyIn1)),
      MathUtils.median(results.map(_.nbDirectedEdgesOnlyIn2))
    )
  }
}
