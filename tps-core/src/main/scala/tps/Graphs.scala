package tps

object Graphs {

  case class Vertex(id: String) extends Serializable {
    override def toString = id
  }

  case class Edge(v1: Vertex, v2: Vertex) extends Serializable {
    override def toString = v1.toString + " - " + v2.toString
  }

  object UndirectedGraph {
    /** Creates a graph from given edges. */
    def apply(E: Iterable[Edge]): UndirectedGraph = {
      val V = E flatMap { case Edge(v1, v2) =>
        Set(v1, v2)
      }
      UndirectedGraph(V.toSet, E.toSet, Set.empty)
    }
  }

  case class UndirectedGraph(
    V: Set[Vertex], 
    E: Set[Edge],
    sources: Set[Vertex]
  ) extends Serializable {
  
    // sanity check
    for (Edge(v1, v2) <- E) {
      assert(v1.id <= v2.id)
    }

    private var neighborMap = Map[Vertex, Set[Vertex]]().withDefaultValue(
      Set.empty[Vertex])
    for (e <- E) {
      neighborMap += e.v1 -> (neighborMap(e.v1) + e.v2)
      neighborMap += e.v2 -> (neighborMap(e.v2) + e.v1)
    }

    override def toString = {
      V.mkString("V = {", ", ", "}") + "\n" +
      E.mkString("E = {", ", ", "}") + "\n" +
      sources.mkString("SRC = {", ", ", "}")
    }

    def neighbors(v: Vertex): Set[Vertex] = neighborMap(v)

    def incidentEdges(v: Vertex): Set[Edge] = {
      val ns = neighbors(v)
      ns.map(n => lexicographicEdge(n, v))
    }

    def contains(e: Edge): Boolean = {
      assert(e.v1.id <= e.v2.id)
      E contains e
    }

    def bfsEdgeOrder: Seq[Edge] = {
      var reachable = sources
      var orderedE = List[Edge]()
      while (orderedE.toSet != E) {
        var newReachable = Set[Vertex]()
        for (e @ Edge(v1, v2) <- E -- orderedE) {
          val v1Reachable = reachable contains v1
          val v2Reachable = reachable contains v2
          if (v1Reachable || v2Reachable) {
            orderedE = orderedE ::: List(e)
            newReachable ++= Set(v1, v2)
          }
        }
        reachable ++= newReachable
      }
      orderedE
    }

    def shortestDistances(
      origin: Set[Vertex]
    ): Map[Vertex, Int] = {
      var alreadySeen = origin
      var toProcess   = origin

      var distance = 0
      var distances = Map[Vertex, Int]()

      for (v <- origin) {
        distances += v -> 0
      }

      while (toProcess.nonEmpty) {
        distance += 1
        val nextStep = toProcess.flatMap { v =>
          this.neighbors(v)
        }.diff(alreadySeen)

        for (v <- nextStep) {
          distances += v -> distance
        }

        alreadySeen ++= nextStep
        toProcess = nextStep
      }

      distances
    }
  }

  type DirectedGraph       = Map[Edge, Set[EdgeDirection]]
  type SignedDirectedGraph = Map[Edge, Set[SignedDirectedEdgeLabel]]

  sealed trait SignedDirectedEdgeLabel
  case class ActiveEdge(direction: EdgeDirection, sign: EdgeSign) extends SignedDirectedEdgeLabel
  case object InactiveEdge extends SignedDirectedEdgeLabel

  sealed trait EdgeDirection
  case object Forward   extends EdgeDirection
  case object Backward  extends EdgeDirection

  sealed trait EdgeSign
  case object Activating  extends EdgeSign
  case object Inhibiting  extends EdgeSign

  // TODO organize into a DirectedGraph entity
  def getDirections(dg: DirectedGraph, src: String, dst: String): Set[EdgeDirection] = {
    val lexicographicOrderFromSrc = src < dst
    val (smallerID, largerID) = if (lexicographicOrderFromSrc) (src, dst) else (dst, src)
    val e = Edge(Vertex(smallerID), Vertex(largerID))
    dg.get(e) match {
      case Some(ds) => {
        if (lexicographicOrderFromSrc) ds else ds.map(reverseDirection)
      }
      case None => {
        Set()
      }
    }
  }

  def toSignedDirectedGraph(dg: DirectedGraph): SignedDirectedGraph = {
    dg map { case (e, ds) =>
      val labelsWithSign = ds.flatMap(d => Set[SignedDirectedEdgeLabel](ActiveEdge(d, Activating), ActiveEdge(d, Inhibiting)))
      e -> labelsWithSign
    }
  }

  def lexicographicEdge(v1: Vertex, v2: Vertex): Edge =
    GraphParsing.lexicographicEdge(v1.id, v2.id)

  private def reverseDirection(d: EdgeDirection): EdgeDirection = d match {
    case Forward => Backward
    case Backward => Forward
  }
}
