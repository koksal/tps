package tps.synthesis

import tps.Graphs._
import tps.ResultReporter
import tps.TabularData

import tps.util.FileUtils

import Events._

object Dominators {

  type State = Set[TimedDomSet]

  case class TimedDomSet(time: Int, dominators: Set[Vertex]) {
    override def toString: String = {
      val domStr = dominators.mkString("{", ",", "}")
      s"T = $time, dom: $domStr" 
    }
  }

}

class DataflowSolver(
  graph: UndirectedGraph,
  partialModel: SignedDirectedGraph,
  opts: SynthesisOptions,
  interp: TriggerInterpretation,
  resultReporter: ResultReporter
) extends Solver {

  import Dominators._

  val V = graph.V
  val E = graph.E

  // state
  var actStates = Map[Vertex, State]()
  var inhStates = Map[Vertex, State]()

  var sol: SignedDirectedGraph = Map.empty

  // tracking changes
  var toProcess : Set[Vertex] = Set.empty
  var dirty     : Set[Vertex] = Set.empty

  def summary(): SignedDirectedGraph = {
    init()

    while (!fixpointReached) {
      for (v <- toProcess) {
        val incident = graph.incidentEdges(v)

        for (e <- incident) {
          updateEdge(e)
        }
      }

      toProcess = dirty
      dirty = Set.empty
    }

    logState()

    sol filter { case (e, ess) => !ess.isEmpty }
  }

  def init() = {
    actStates = Map.empty
    inhStates = Map.empty
    sol       = Map.empty

    val initReachable = if (opts.constraintOptions.connectivity) graph.sources else graph.V
    toProcess         = initReachable
    dirty             = Set.empty

    for (v <- initReachable) {
      val ae = Event(v, Activation)
      val ie = Event(v, Inhibition)
      
      actStates += v -> initState(ae)
      inhStates += v -> initState(ie)
    }
  }

  def fixpointReached: Boolean = toProcess.isEmpty

  def logState(): Unit = {
    def activationTuple(v: Vertex): Seq[String] = {
      val actSets = actStates.getOrElse(v, Set())
      val inhSets = inhStates.getOrElse(v, Set())

      val actTimes = actSets map (_.time)
      val inhTimes = inhSets map (_.time)

      val nbWindows = interp.timeSeries.nbMeasurements - 1

      val tupleWindowValues = for (wIndex <- 1 to nbWindows) yield {
        if (actTimes(wIndex) && inhTimes(wIndex)) {
          "ambiguous" 
        } else if (actTimes(wIndex)) {
          "activation"
        } else if (inhTimes(wIndex)) {
          "inhibition"
        } else {
          "inactive"
        }
      }
      v.id +: tupleWindowValues
    }
    val tuples = graph.V.toSeq map activationTuple
    val labels = "peptide" +: interp.timeSeries.labels.tail
    val data = TabularData(labels, tuples)

    resultReporter.writeOutput("activity-windows.tsv", data.toTSVString())
  }

  def updateEdge(e: Edge) = {
    val toTest = partialModel.get(e) match {
      case Some(partialEdgeSol) => 
        val activeEdgeSol: Set[ActiveEdge] = partialEdgeSol collect {
          case s: ActiveEdge => s
        }
        allEdgeSolutions intersect activeEdgeSol
      case None => allEdgeSolutions
    }
    val validSol = toTest filter { ae => updateEdgeForSol(e, ae) }
    sol += e -> (Set[SignedDirectedEdgeLabel]() ++ validSol)
  }

  def updateEdgeForSol(e: Edge, ae: ActiveEdge) =  {
    val (src, dst) = ae.direction match {
      case Forward => (e.v1, e.v2)
      case Backward => (e.v2, e.v1)
    }
    updateEdgeForSign(src, dst, ae.sign)
  }

  def updateEdgeForSign(src: Vertex, dst: Vertex, s: EdgeSign): Boolean = s match {
    case Activating => {
      val wu1 = checkStates(src.activation precedes dst.activation)
      val wu2 = checkStates(src.inhibition precedes dst.inhibition)
      wu1 || wu2
    }
    case Inhibiting => {
      val wu1 = checkStates(src.activation precedes dst.inhibition)
      val wu2 = checkStates(src.inhibition precedes dst.activation)
      wu1 || wu2
    }
  }

  def checkStates(eo: EventOrder): Boolean = eo match {
    case Precedes(fstEvent @ Event(fstV, fstET), sndEvent @ Event(sndV, sndET)) => {
      // fetch states for each
      val fstState = eventState(fstEvent)
      val sndState = eventState(sndEvent)
      val transferredState = transfer(fstState, sndEvent)

      if (transferredState.isEmpty) {
        false
      } else {
        val j = join(transferredState, sndState)
        updateEventState(sndEvent, j)
        if (sndState != j) dirty += sndV
        true
      }
    }
  }

  def transfer(in: State, localEvent: Event): State = {
    def validPredecessor(d: TimedDomSet, t: Int): Boolean = {
      d.time <= t && !d.dominators.contains(localEvent.v)
    }
    val s2 = stateFromProfile(localEvent)
    val succ = s2 filter { case TimedDomSet(t, ds) =>
      in.exists(validPredecessor(_, t))
    }
    succ map { case TimedDomSet(t, ds) =>
      val preceding = in.filter(validPredecessor(_, t))
      val precDs = preceding map (_.dominators)
      val commonPrecDs = precDs.reduceLeft[Set[Vertex]]{ case (acc, toAdd) =>
        acc intersect toAdd
      }
      TimedDomSet(t, commonPrecDs + localEvent.v)
    }
  }

  def join(s1: State, s2: State): State = {
    val ts1 = s1.map(_.time)
    val ts2 = s2.map(_.time)
    val unionTs = ts1 ++ ts2
    val newTDS = for (t <- unionTs) yield {
      (s1.find(_.time == t), s2.find(_.time == t)) match {
        case (Some(tds1), Some(tds2)) => TimedDomSet(t, tds1.dominators intersect tds2.dominators)
        case (Some(tds1), None) => tds1
        case (None, Some(tds2)) => tds2
        case (None, None) => throw new Exception("Cannot happen.")
      }
    }
    newTDS
  }

  def eventState(e: Event): State = {
    eventStateOpt(e).getOrElse(Set.empty)
  }

  def eventStateOpt(e: Event): Option[State] = e match {
    case Event(v, Activation) => actStates.get(v)
    case Event(v, Inhibition) => inhStates.get(v)
  }
  
  def updateEventState(e: Event, s: State) = e match {
    case Event(v, Activation) => actStates += v -> s
    case Event(v, Inhibition) => inhStates += v -> s
  }

  def initState(e: Event): State = {
    val is = profileIntervals(e).toSet
    val tds = is map (i => TimedDomSet(i, Set(e.v)))
    tds
  }

  def stateFromProfile(e: Event): State = {
    val is = profileIntervals(e).toSet
    val tds = is map (i => TimedDomSet(i, V))
    tds
  }

  def profileIntervals(e: Event): Seq[Int] = e match {
    case Event(v, et) => {
      if (opts.constraintOptions.temporality) {
        interp.vertexPattern(v) match {
          case None => {
            // unconstrained due to lack of data
            allIntervals
          }
          case Some(profile) => {
            et match {
              case Activation => interp.allowedActivationIntervals(profile)
              case Inhibition => interp.allowedInhibitionIntervals(profile)
            }
          }
        }
      } else {
        // unconstrained because constraint is turned off
        allIntervals
      }
    }
  }

  def allIntervals: Seq[Int] = {
    val min = 1
    val max = interp.timeSeries.nbMeasurements - 1
    1 to max
  }

  def unconstrainedState(dominators: Set[Vertex]): State = {
    val tds = allIntervals.map(i => TimedDomSet(i, dominators)).toSet
    tds
  }

  def allEdgeSolutions: Set[ActiveEdge] = {
    for {
      d <- Set(Forward, Backward)
      s <- Set(Activating, Inhibiting)
    } yield ActiveEdge(d, s)
  }

}
