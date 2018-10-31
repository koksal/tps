package tps.evaluation

import tps._
import tps.Graphs._
import tps.SignedDirectedGraphOps._
import parsing.DBNNetworkParser
import java.io.File

import tps.util.MathUtils

object NetworkReferenceComparison {
  def main(args: Array[String]): Unit = {
    // first argument: reference network
    val referenceFile = new File(args(0))
    val referenceName = referenceFile.getName()
    val (referenceNetwork, referenceEvidences) = ReferenceParser.run(
      referenceFile)

    // rest of arguments: network files to use for average/median analysis
    val networkFiles = args.tail map (a => new File(a))
    val networks = networkFiles map (f => SignedDirectedGraphParser.run(f))
    println(s"Parsed ${networks.size} networks.")

    val results = networks.zipWithIndex map {
      case (network, i) => {
        compare(network, referenceNetwork, s"network-$i", referenceName)
      }
    }
    println(results.mkString("\n"))

    // compute aggregate metrics
    val aggregateResult = aggregateResults(results)
    println(aggregateResult)
  }

  def resultString(res: NetworkReferenceComparisonResult): String = {
    ???
  }

  def aggregateResults(
    results: Iterable[NetworkReferenceComparisonResult]
  ): NetworkReferenceComparisonResult = {
    assert(results.forall(_.referenceName == results.head.referenceName))

    NetworkReferenceComparisonResult(
      "aggregate-results",
      results.head.referenceName,
      MathUtils.median(results.map(_.nbCandidateEdges)),
      results.head.nbReferenceEdges,
      MathUtils.median(results.map(_.nbCommonEdges)),
      MathUtils.average(results.map(_.undirectedPrecision)),
      MathUtils.average(results.map(_.undirectedRecall)),
      MathUtils.median(results.map(_.nbDirectedEdges)),
      MathUtils.median(results.map(_.directedEdgeRatio)),
      MathUtils.median(results.map(_.nbCommonDirectedEdges)),
      MathUtils.median(results.map(_.commonDirectedEdgeRatio)),
      MathUtils.median(results.map(_.nbMatchingDirectionEdges)),
      MathUtils.median(results.map(_.matchingDirectionEdgeRatio)),
      MathUtils.median(results.map(_.nbConflictingDirectionEdges)),
      MathUtils.median(results.map(_.conflictingDirectionEdgeRatio)),
      MathUtils.median(results.map(_.nbUnconfirmedDirectionEdges)),
      MathUtils.median(results.map(_.unconfirmedDirectionEdgeRatio))
    )
  }

  // legacy analysis
  def runComparativeAnalysis(refFn: String): Unit = {
    val refFile = new File(refFn)
    val refName = refFile.getName()
    val (refNetwork, refEvidence) = ReferenceParser.run(refFile)

    val networkFolder = new File("data/networks/evaluation")

    val dbnFn = s"$networkFolder/DBN.tsv"
    val dbnFile = new File(dbnFn)
    val dbnMinProb = 0.025
    val dbnNetwork = DBNNetworkParser.run(dbnFile, dbnMinProb)

    // PIN + kin-sub edges
    val pinFn = "data/networks/directed-pin-with-resource-edges.tsv"
    val pinFile = new File(pinFn)
    val pin = toSignedDirectedGraph(PINParser.run(pinFile))

    // SIF networks
    // undirected TXN + TPS (time series + kinase-substrate)
    val sifFiles = networkFolder.listFiles() filter { f =>
      f.getName().endsWith(".sif")
    }
    val sifNetworks = sifFiles map { f =>
      f.getName() -> SignedDirectedGraphParser.run(f)
    }

    val candidates = Map(
      "PIN" -> pin,
      "DBN" -> dbnNetwork
    ) ++ sifNetworks

    for ((id, network) <- candidates) {
      println(resultString(compare(network, refNetwork, id, refName)))
    }
  }

  // Use doubles instead of integers to compute aggregate statistics
  case class NetworkReferenceComparisonResult(
    candidateName: String,
    referenceName: String,
    nbCandidateEdges: Double,
    nbReferenceEdges: Double,
    nbCommonEdges: Double,
    undirectedPrecision: Double,
    undirectedRecall: Double,
    nbDirectedEdges: Double,
    directedEdgeRatio: Double,
    nbCommonDirectedEdges: Double,
    commonDirectedEdgeRatio: Double,
    nbMatchingDirectionEdges: Double,
    matchingDirectionEdgeRatio: Double,
    nbConflictingDirectionEdges: Double,
    conflictingDirectionEdgeRatio: Double,
    nbUnconfirmedDirectionEdges: Double,
    unconfirmedDirectionEdgeRatio: Double
  )

  def compare(
    candidate: SignedDirectedGraph, 
    reference: SignedDirectedGraph, 
    candidateName: String,
    referenceName: String
  ) = {
    // compute precision and recall where relevance is whether a selected edge
    // is in the reference

    val candidateE = candidate.keySet
    val referenceE = reference.keySet

    val commonE = candidateE intersect referenceE
    var undirPrecision = commonE.size.toDouble / candidateE.size.toDouble
    if (candidateE.isEmpty) undirPrecision = 0.0
    val undirRecall    = commonE.size.toDouble / referenceE.size.toDouble

    val directedE = candidateE filter { e =>
      oneActiveDirection(candidate(e))
    }

    val commonDirectedE = commonE filter { e =>
      oneActiveDirection(candidate(e))
    }

    val matchingDirectionE  = commonE filter { e =>
      val candidateL = candidate(e)
      val referenceL = reference(e)

      onlyForward(candidateL) && onlyForward(referenceL) ||
      onlyBackward(candidateL) && onlyBackward(referenceL)
    }
    
    val conflictingE = commonE filter { e =>
      val candidateL = candidate(e)
      val referenceL = reference(e)

      onlyForward(candidateL) && onlyBackward(referenceL) ||
      onlyBackward(candidateL) && onlyForward(referenceL)
    }

    val unconfirmedDirectionE = commonE filter { e =>
      oneActiveDirection(candidate(e)) && ambiguousDirection(reference(e))
    }

    NetworkReferenceComparisonResult(
      candidateName,
      referenceName,
      candidateE.size,
      referenceE.size,
      commonE.size,
      undirPrecision,
      undirRecall,
      directedE.size,
      directedE.size.toDouble / candidateE.size,
      commonDirectedE.size,
      commonDirectedE.size.toDouble / directedE.size.toDouble,
      matchingDirectionE.size,
      matchingDirectionE.size.toDouble / directedE.size.toDouble,
      conflictingE.size,
      conflictingE.size.toDouble / directedE.size.toDouble,
      unconfirmedDirectionE.size,
      unconfirmedDirectionE.size.toDouble / directedE.size.toDouble
    )
  }

}
