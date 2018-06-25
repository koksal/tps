package tps.evaluation

import tps._
import tps.Graphs._
import tps.SignedDirectedGraphOps._
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
      MathUtils.median(results.map(_.nbDirectedEdges)),
      MathUtils.median(results.map(_.nbCommonDirectedEdges)),
      MathUtils.median(results.map(_.nbMatchingDirectionEdges)),
      MathUtils.median(results.map(_.nbConflictingDirectionEdges)),
      MathUtils.median(results.map(_.nbUnconfirmedDirectionEdges))
    )
  }

  // Use doubles instead of integers to compute aggregate statistics
  case class NetworkReferenceComparisonResult(
    candidateName: String,
    referenceName: String,
    nbCandidateEdges: Double,
    nbReferenceEdges: Double,
    nbCommonEdges: Double,
    nbDirectedEdges: Double,
    nbCommonDirectedEdges: Double,
    nbMatchingDirectionEdges: Double,
    nbConflictingDirectionEdges: Double,
    nbUnconfirmedDirectionEdges: Double
  ) {
    override def toString: String = {
      val values = List(
        referenceName,
        candidateName,
        nbReferenceEdges,
        nbCandidateEdges,
        nbCommonEdges,
        metricString(precisionLowerBound(nbCommonEdges, nbCandidateEdges)),
        metricString(recall(nbCommonEdges, nbReferenceEdges)),
        nbDirectedEdges,
        nbCommonDirectedEdges,
        nbMatchingDirectionEdges,
        nbConflictingDirectionEdges,
        nbUnconfirmedDirectionEdges,
        metricString(
          precisionLowerBound(nbMatchingDirectionEdges, nbCommonDirectedEdges)),
        metricString(
          precisionUpperBound(nbConflictingDirectionEdges,  nbCommonDirectedEdges)),
        metricString(
          nbMatchingDirectionEdges / nbDirectedEdges * 1000.0),
        metricString(
          nbConflictingDirectionEdges / nbDirectedEdges * 1000.0)
      )

      values.mkString(",")
    }
  }

  def metricString(precision: Double): String = {
    if (precision.isNaN) "N/A" else precision.toString
  }

  def precisionLowerBound(
    nbPos: Double,
    nbPredictions: Double
  ): Double = {
    nbPos / nbPredictions
  }

  def precisionUpperBound(
    nbNeg: Double,
    nbPredictions: Double
  ): Double = {
    (nbPredictions - nbNeg) / nbPredictions
  }

  def recall(
    nbPos: Double,
    nbTotal: Double
  ): Double = {
    nbPos / nbTotal
  }

  def compare(
    candidate: SignedDirectedGraph, 
    reference: SignedDirectedGraph, 
    candidateName: String,
    referenceName: String
  ): NetworkReferenceComparisonResult = {
    val candidateE = candidate.keySet
    val referenceE = reference.keySet

    val commonE = candidateE intersect referenceE

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
      directedE.size,
      commonDirectedE.size,
      matchingDirectionE.size,
      conflictingE.size,
      unconfirmedDirectionE.size
    )
  }

}
