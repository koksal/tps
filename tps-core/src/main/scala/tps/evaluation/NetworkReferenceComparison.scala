package tps.evaluation

import tps._
import tps.Graphs._
import tps.SignedDirectedGraphOps._

import parsing.DBNNetworkParser

import java.io.File

object NetworkReferenceComparison {
  def main(args: Array[String]): Unit = {
    val refFn = args(0)
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
      compare(network, refNetwork, id, refName, refEvidence)
    }
  }

  def compare(
    candidate: SignedDirectedGraph, 
    reference: SignedDirectedGraph, 
    candidateName: String,
    referenceName: String,
    evidence: Map[Edge, String]
  ) = {
    // compute precision and recall where relevance is whether a selected edge is in the reference

    val candidateE = candidate.keySet
    val referenceE = reference.keySet

    val commonE = candidateE intersect referenceE
    val undirPrecision = commonE.size.toDouble / candidateE.size.toDouble
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

    val row = List(
      referenceName,
      candidateName,
      referenceE.size,
      candidateE.size,
      commonE.size,
      undirPrecision,
      undirRecall,
      directedE.size,
      commonDirectedE.size,
      matchingDirectionE.size,
      conflictingE.size,
      unconfirmedDirectionE.size
    )
    println(row.mkString("\t"))

    val conflictFile = new File(s"conflicts-$candidateName-$referenceName.tsv")
    // write contradictions into file
    val conflictRows = for (e <- conflictingE) yield {
      val ev = evidence(e)
      List(e.v1, e.v2, ev).mkString("\t")
    }
    // util.FileUtils.writeToFile(conflictFile, conflictRows.mkString("\n"))
  }
}
