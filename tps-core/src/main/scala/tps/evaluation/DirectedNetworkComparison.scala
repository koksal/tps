package tps.evaluation

import java.io.File

import tps.Graphs.SignedDirectedEdgeLabel
import tps.Graphs.SignedDirectedGraph
import tps.util.MathUtils
import tps.{SignedDirectedGraphOps, SignedDirectedGraphParser}

object DirectedNetworkComparison {
  def main(args: Array[String]): Unit = {
    // first argument: prior knowledge network
    val priorKnowledgeFile = new File(args.head)
    val priorKnowledgeNetwork = SignedDirectedGraphParser.run(
      priorKnowledgeFile)

    // second argument: reference signed directed network
    // rest of arguments: collection of signed directed networks to compare to
    val allNetworkFiles = args.tail map (arg => new File(arg))
    val allNetworks = allNetworkFiles map (
      f => SignedDirectedGraphParser.run(f))

    val firstNetwork = allNetworks.head
    val restNetworks = allNetworks.tail
    println(s"Comparing first network to ${restNetworks.size} networks.")

    val results = restNetworks map { n =>
      runComparison(firstNetwork, n, priorKnowledgeNetwork)
    }

    val aggregateResult = aggregateResults(results)
    println(aggregateResult)
  }

  case class SignedDirectedGraphComparisonResult(
    directedComparison: ComparisonSubResult,
    signedDirectedComparison: ComparisonSubResult
  ) {
    override def toString(): String = {
      s"""Directed comparison:
        |${directedComparison}
        |Signed directed comparison:
        |${signedDirectedComparison}
      """.stripMargin
    }
  }

  case class ComparisonSubResult(
    nb1: Double,
    nb2: Double,
    nbCommon: Double,
    nbCommonInPrior: Double,
    nbAgreeing: Double,
    nbConflicting: Double,
    nbOnly1: Double,
    nbOnly2: Double
  ) {
    override def toString(): String = {
      val rows = List(
        s"Nb. edges in 1: ${nb1}",
        s"Nb. edges in 2: ${nb2}",
        s"Nb. common: ${nbCommon}",
        s"Nb. common in prior: ${nbCommonInPrior}",
        s"Nb. agreeing: ${nbAgreeing}",
        s"Nb. conflicting: ${nbConflicting}",
        s"Nb. only in 1: ${nbOnly1}",
        s"Nb. only in 2: ${nbOnly2}"
      )
      rows.mkString("\n")
    }
  }

  private def runComparison(
    n1: SignedDirectedGraph,
    n2: SignedDirectedGraph,
    priorKnowledge: SignedDirectedGraph
  ): SignedDirectedGraphComparisonResult = {
    val directedComp = compareSubResults(n1, n2, priorKnowledge,
      SignedDirectedGraphOps.oneActiveDirection,
      SignedDirectedGraphOps.sameUniqueDirection,
      SignedDirectedGraphOps.conflictingUniqueActiveDirection
    )

    val signedDirectedComp = compareSubResults(n1, n2, priorKnowledge,
      SignedDirectedGraphOps.oneActiveEdge,
      SignedDirectedGraphOps.sameUniqueActiveEdge,
      SignedDirectedGraphOps.confictingUniqueActiveEdge
    )

    SignedDirectedGraphComparisonResult(directedComparison = directedComp,
      signedDirectedComparison = signedDirectedComp)
  }

  private def compareSubResults(
    n1: SignedDirectedGraph,
    n2: SignedDirectedGraph,
    priorKnowledge: SignedDirectedGraph,
    filter: Set[SignedDirectedEdgeLabel] => Boolean,
    agreementFun:
      (Set[SignedDirectedEdgeLabel], Set[SignedDirectedEdgeLabel]) => Boolean,
    conflictFun:
      (Set[SignedDirectedEdgeLabel], Set[SignedDirectedEdgeLabel]) => Boolean
  ): ComparisonSubResult = {
    val edges1 = n1.collect{
      case (e, ess) if filter(ess) => e
    }.toSet
    val edges2 = n2.collect{
      case (e, ess) if filter(ess) => e
    }.toSet

    val commonE = edges1.intersect(edges2)
    val commonEInPrior = commonE.intersect(priorKnowledge.keySet)

    val agreeing = commonE filter { e =>
      agreementFun(n1(e), n2(e))
    }
    val conflicting = commonE filter { e =>
      conflictFun(n1(e), n2(e))
    }

    val only1 = edges1 -- edges2
    val only2 = edges2 -- edges1

    ComparisonSubResult(
      nb1 = edges1.size,
      nb2 = edges2.size,
      nbCommon = commonE.size,
      nbCommonInPrior = commonEInPrior.size,
      nbAgreeing = agreeing.size,
      nbConflicting = conflicting.size,
      nbOnly1 = only1.size,
      nbOnly2 = only2.size
    )
  }

  private def aggregateResults(
    results: Iterable[SignedDirectedGraphComparisonResult]
  ): SignedDirectedGraphComparisonResult = {
    SignedDirectedGraphComparisonResult(
      directedComparison =
        aggregateSubResults(results.map(_.directedComparison)),
      signedDirectedComparison =
        aggregateSubResults(results.map(_.signedDirectedComparison))
    )
  }

  private def aggregateSubResults(
    results: Iterable[ComparisonSubResult]
  ): ComparisonSubResult = {
    ComparisonSubResult(
      MathUtils.median(results.map(_.nb1)),
      MathUtils.median(results.map(_.nb2)),
      MathUtils.median(results.map(_.nbCommon)),
      MathUtils.median(results.map(_.nbCommonInPrior)),
      MathUtils.median(results.map(_.nbAgreeing)),
      MathUtils.median(results.map(_.nbConflicting)),
      MathUtils.median(results.map(_.nbOnly1)),
      MathUtils.median(results.map(_.nbOnly2))
    )
  }
}
