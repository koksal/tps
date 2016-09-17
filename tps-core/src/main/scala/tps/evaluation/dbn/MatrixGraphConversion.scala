package tps.evaluation.dbn

import tps._
import tps.Graphs._

object MatrixGraphConversion {
  def main(args: Array[String]): Unit = {
    val opts = ArgHandling.parseOptions(args)

    val pinPath = new java.io.File("data/networks/directed-pin-with-resource-edges.tsv")
    val pin = PINParser.run(pinPath)
    val timeSeries    = TimeSeriesParser.run(opts.timeSeriesPath)
    val peptideProteinMap = opts.peptideProteinMapPath map { p =>
      PeptideProteinMappingParser.run(p)
    } getOrElse Map.empty
    val reporter = new FileReporter(opts.outFolder, opts.outLabel)

    readGraphFromMatrix(timeSeries, peptideProteinMap, reporter)
  }

  def createAdjacencyMatrix(
    pin: DirectedGraph,
    timeSeries: TimeSeries,
    peptideProteinMap: Map[String, Set[String]],
    reporter: FileReporter
  ): Unit = {
    // TODO could add to CLI arguments
    val adjMatrix = AdjacencyMatrix.create(timeSeries, pin, peptideProteinMap)
    val tsvContent = adjMatrix.toTabularData.toTSVString(printHeaders = false)

    reporter.output("adjacencyMatrix.tsv", tsvContent)
  }

  def readGraphFromMatrix(
    timeSeries: TimeSeries,
    peptideProteinMap: Map[String, Set[String]],
    reporter: FileReporter
  ) = {
    val edgeProbabilityFile = new java.io.File("edge_prob_matrix.tsv")
    val interactionSignFile = new java.io.File("interaction_sign_matrix.tsv")

    val edgeProbData        = new TSVSource(edgeProbabilityFile, noHeaders = true).data
    val interactionSignData = new TSVSource(interactionSignFile, noHeaders = true).data

    val edgeProbMatrix        = edgeProbData.tuples.toIndexedSeq.map(_.toIndexedSeq)
    val interactionSignMatrix = interactionSignData.tuples.toIndexedSeq.map(_.toIndexedSeq)

    var labels = timeSeries.profiles.map(_.id).toIndexedSeq

    // TODO handle self edges
    val tuples = for {
      (peptide1, i) <- labels.zipWithIndex
      (peptide2, j) <- labels.zipWithIndex 
      if i != j
    } yield {
      val prots1 = peptideProteinMap.getOrElse(peptide1, Set[String]())
      val prots2 = peptideProteinMap.getOrElse(peptide2, Set[String]())

      assert(prots1.size == 1, s"prots for $peptide1: $prots1")
      assert(prots2.size == 1, s"prots for $peptide2: $prots2")

      val prob = edgeProbMatrix(i)(j)
      val sign = interactionSignMatrix(i)(j)

      // TODO adjust direction in terms of protein lexicographic order
      val edgeTuples = for {
        prot1 <- prots1
        prot2 <- prots2
        if prot1 != prot2
      } yield {
        Seq(prot1, prot2, prob, sign, peptide1, peptide2)
      }

      edgeTuples
    }

    val fields = Seq("protein 1", "protein 2", "probability", "sign", "peptide 1", "peptide 2")
    val outputData = TabularData(fields, tuples.flatten)

    reporter.output("matrix_network.tsv", outputData.toTSVString())
  }
}
