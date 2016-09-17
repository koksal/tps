package tps.synthesis

import scala.collection.mutable.{Map => MutableMap}

import Trees._
import TypeTrees._

import tps.Graphs._
import tps.Profile
import tps.TimeSeries

import tps.util.FileUtils
import tps.util.LogUtils
import tps.util.MathUtils

class TriggerInterpretation(
  useMonotonicity: Boolean,
  graph: UndirectedGraph,
  ts: TimeSeries,
  firstScores: Map[String, Seq[Double]],
  prevScores: Map[String, Seq[Double]],
  threshold: Double
) extends Interpretation {
  import Events._

  val timeSeries: TimeSeries = ts

  private def allowedIntervals(
    p: Profile,
    eventType: EventType,
    threshold: Double,
    firstScores: Seq[Double],
    prevScores: Seq[Double]
  ): Seq[Int] =  {
    assert(firstScores.size == prevScores.size)
    assert(p.values.size == firstScores.size + 1)

    var allowed = List[Int]()
    var all     = List[Int]()

    val firstAndPrev = firstScores zip prevScores
    val valuesAndPValues = p.values.tail zip firstAndPrev

    def getValueOrComplain(vOpt: Option[Double]): Double = {
      vOpt.getOrElse(throw new Exception(s"Problem processing profile for ${p.id}"))
    }

    for (((vOpt, (fs, ps)), zipIndex) <- valuesAndPValues.zipWithIndex) {
      val i = zipIndex + 1

      val isAllowed = vOpt match {
        case Some(v) => {
          val firstSignificant = fs < threshold
          val firstValid = firstSignificant && {
            val firstValidDirection = eventType match {
              case Activation => v > getValueOrComplain(p.values.head)
              case Inhibition => v < getValueOrComplain(p.values.head)
            }

            val extremumSoFar = eventType match {
              case Activation => p.values.take(i).filter(_.isDefined).forall(getValueOrComplain(_) < v)
              case Inhibition => p.values.take(i).filter(_.isDefined).forall(getValueOrComplain(_) > v)
            }

            if (useMonotonicity) {
              firstValidDirection && extremumSoFar
            } else {
              firstValidDirection
            }
          }

          val prevSignificant = ps < threshold
          val prevValid = prevSignificant && {
            val prevValidDirection = eventType match {
              case Activation => v > getValueOrComplain(p.values(i - 1))
              case Inhibition => v < getValueOrComplain(p.values(i - 1))
            }

            prevValidDirection
          }

          firstValid || prevValid
        }
        case None => false // the time point has no value
      }

      if (isAllowed) { allowed = allowed ++ List(i) }
      all = all ++ List(i)
    }

    val significantValueExists = (firstScores ++ prevScores).exists(_ < threshold)
    if (significantValueExists) allowed else all
  }

  val allowedActivationIntervals: Map[Profile, Seq[Int]] = {
    val is = timeSeries.profiles map { p => 
        (p, allowedIntervals(p, Activation, threshold, firstScores(p.id), prevScores(p.id)))
    }
    is.toMap
  }

  val allowedInhibitionIntervals: Map[Profile, Seq[Int]] = {
    val is = timeSeries.profiles map (p => 
        (p, allowedIntervals(p, Inhibition, threshold, firstScores(p.id), prevScores(p.id))))
    is.toMap
  }

  // TODO is there a way to push this up the class hierarchy?
  def makeSymbolic(): SymbolicTriggerInterpretation = {
    new SymbolicTriggerInterpretation(graph, this)
  }

}

object Events {
  sealed trait EventType
  case object Activation extends EventType
  case object Inhibition extends EventType
  case class Event(v: Vertex, tpe: EventType) {
    def precedes(that: Event): EventOrder = Precedes(this, that)
  }
  sealed trait EventOrder
  case class Precedes(fst: Event, snd: Event) extends EventOrder
  sealed trait Relationship
  case class Activates(src: Vertex, dst: Vertex, symG: SymbolicGraph) extends Relationship
  case class Inhibits(src: Vertex, dst: Vertex, symG: SymbolicGraph) extends Relationship

  implicit class RichVertex(v: Vertex) {
    def activation: Event = Event(v, Activation)
    def inhibition: Event = Event(v, Inhibition)
    def activates(other: Vertex)(implicit sg: SymbolicGraph): Relationship = Activates(v, other, sg)
    def inhibits(other: Vertex)(implicit sg: SymbolicGraph): Relationship  = Inhibits(v, other, sg)
  }
}

class SymbolicTriggerInterpretation (
  g: UndirectedGraph,
  bi: TriggerInterpretation
) extends SymbolicInterpretation {
  import Events._
  import scala.language.postfixOps
  import scala.language.implicitConversions

  val graph: UndirectedGraph = g
  val baseInterpretation: TriggerInterpretation = bi
  
  var activationIntervalVars: Map[Vertex, Variable] = Map.empty
  var inhibitionIntervalVars: Map[Vertex, Variable] = Map.empty
  var activationItvlOrderVars: Map[Vertex, Variable] = Map.empty
  var inhibitionItvlOrderVars: Map[Vertex, Variable] = Map.empty

  private def eventIntervalAndOrderVars(e: Event): (Variable, Variable) = e match {
    case Event(v, Activation) => (activationIntervalVars(v), activationItvlOrderVars(v))
    case Event(v, Inhibition) => (inhibitionIntervalVars(v), inhibitionItvlOrderVars(v))
  }

  // prepare variables and constraint to base interpretation
  prepareTriggerIntervalVars()
  prepareTriggerOrderVars()

  def consistency(): Expr = {
    chooseAllowedIntervals()
  }

  private def prepareTriggerIntervalVars(): Unit = {
    for (v <- graph.V) {
      activationIntervalVars += 
        ((v, FreshIdentifier(v.id + "-act").setType(IntType).toVariable))
      inhibitionIntervalVars += 
        ((v, FreshIdentifier(v.id + "-inh").setType(IntType).toVariable))
    }
  }

  private def prepareTriggerOrderVars(): Unit = {
    for (v <- graph.V) {
      activationItvlOrderVars += 
        ((v, FreshIdentifier(v.id + "-act-order").setType(IntType).toVariable))
      inhibitionItvlOrderVars += 
        ((v, FreshIdentifier(v.id + "-inh-order").setType(IntType).toVariable))
    }
  }

  private def chooseAllowedIntervals(): Expr = {
    def actFun(p: Profile) = baseInterpretation.allowedActivationIntervals(p)
    def inhFun(p: Profile) = baseInterpretation.allowedInhibitionIntervals(p)

    val actMap = activationIntervalVars
    val inhMap = inhibitionIntervalVars

    val allowedAct = chooseAllowedIntervals(actFun, actMap)
    val allowedInh = chooseAllowedIntervals(inhFun, inhMap)

    val triggering = atMostOneTriggering()

    And(allowedAct, allowedInh, triggering)
  }

  private def chooseAllowedIntervals(
      allowedForProfile: (Profile) => Seq[Int],
      triggerIntervalVars: Map[Vertex, Variable]): Expr = {
    val allCnstr = for (v <- graph.V) yield {
      val vertexCnstr = vertexPattern(v) match {
        case Some(p) => {
          val allowed = allowedForProfile(p)
          // represent no activation by value 0
          val doesNotTrigger = Equals(triggerIntervalVars(v), IntLiteral(0))
          if (allowed.isEmpty) {
            doesNotTrigger
          } else {
            // triggering may happen in one of the permitted intervals
            val isInAllowedIntervals = allowed map {
              i => Equals(triggerIntervalVars(v), IntLiteral(i))
            }
            Or(isInAllowedIntervals :+ doesNotTrigger: _*)
          }
        }
        case None => {
          // allow any within bounds, including inactive (0)
          val withinBounds = And(
              GreaterEquals(triggerIntervalVars(v), IntLiteral(0)),
              LessThan(
                triggerIntervalVars(v), 
                IntLiteral(baseInterpretation.timeSeries.nbMeasurements))
            )
          withinBounds
        }
      }
      vertexCnstr
    }
    And(allCnstr.toSeq: _*)
  }

  def atMostOneTriggering(): Expr = {
    val cnstrs = for (v <- graph.V) yield {
      val notActivated = Equals(activationIntervalVars(v), IntLiteral(0))
      val notInhibited = Equals(inhibitionIntervalVars(v), IntLiteral(0))
      Or(notActivated, notInhibited)
    }
    And(cnstrs.toSeq: _*)
  }

  implicit private def eventOrderFormula(eo: EventOrder): Expr = eo match {
    case Precedes(fst, snd) => {
      val (fstItvl, fstItvlOrder) = eventIntervalAndOrderVars(fst)
      val (sndItvl, sndItvlOrder) = eventIntervalAndOrderVars(snd)
      // zero means non-activated
      val bothNonZero = And(
        Not(Equals(fstItvl, IntLiteral(0))),
        Not(Equals(sndItvl, IntLiteral(0)))
      )
      val fstFiresAtLeastInSameItvl = 
        LessEquals(
          fstItvl,
          sndItvl
        )
      val fstFiresFirstAtSameInterval = 
        Implies(
          Equals(
            fstItvl,
            sndItvl),
          LessThan(fstItvlOrder, sndItvlOrder)
        )
      And(
        bothNonZero,
        fstFiresAtLeastInSameItvl, 
        fstFiresFirstAtSameInterval
      )
    }
  }

  implicit private def relationshipFormula(r: Relationship): Expr = r match {
    case Activates(src, dst, sg) => sg.isActivatorVars(dst)(src)
    case Inhibits(src, dst, sg) => sg.isInhibitorVars(dst)(src)
  }

  def consistencyWithGraph(implicit sg: SymbolicGraph): Expr = {
    val conditions = for (
      dst <- graph.V;
      src <- graph.neighbors(dst)
    ) yield {

      val activationCond = Implies(
        src activates dst,
        Or(
          src.activation precedes dst.activation,
          src.inhibition precedes dst.inhibition
        )
      )

      val inhibitionCond = Implies(
        src inhibits dst,
        Or(
          src.activation precedes dst.inhibition,
          src.inhibition precedes dst.activation
        )
      )

      And(activationCond, inhibitionCond)
    }

    And(conditions.toList: _*)
  }

  def solution(m: Map[Identifier, Expr]): TriggerSolution = {
    val intervalValues = for ((v, ast) <- activationIntervalVars) yield {
      val evaluated = m.get(ast.id) match {
        case Some(IntLiteral(v)) => v
        case Some(_) => LogUtils.terminate("Type mismatch.")
        case None => -1
      }
      (v, evaluated)
    }
    val orderValues = for ((v, ast) <- activationItvlOrderVars) yield {
      val evaluated = m.get(ast.id) match {
        case Some(IntLiteral(v)) => v
        case Some(_) => LogUtils.terminate("Type mismatch.")
        case None => -1
      }
      (v, evaluated)
    }
    TriggerSolution(intervalValues, orderValues)
  }

  def solutionFormula(sol: InterpretationSolution): Expr = sol match {
    case TriggerSolution(intervals, orderIndices) => {
      val intervalConjuncts = for ((vertex, value) <- intervals) yield {
        Equals(activationIntervalVars(vertex), IntLiteral(value))
      }
      val orderConjuncts = for ((vertex, value) <- orderIndices) yield {
        Equals(activationItvlOrderVars(vertex), IntLiteral(value))
      }
      And((intervalConjuncts.toSeq ++ orderConjuncts.toSeq): _*)
    }
    case _ => LogUtils.terminate("Incompatible solution.")
  }
}

case class TriggerSolution(
  val intervals: Map[Vertex, Int],
  val orderIndices: Map[Vertex, Int]
) extends InterpretationSolution
