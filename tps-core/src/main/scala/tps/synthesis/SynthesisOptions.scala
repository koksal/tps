package tps.synthesis

case class SynthesisOptions(
  solver:                         String = "dataflow",
  collapsePeptides:               Boolean = true,
  pathLengthSlack:                Option[Int] = None,

  // constraints
  constraintOptions:              ConstraintOptions = ConstraintOptions(),

  // solver options
  // TODO move to SolverOption interface, so each solver has its own 
  bitvectorWidth:                 Option[Int] = None,
  bilateralTimeout:               Option[Int] = None
)

case class ConstraintOptions(
  connectivity: Boolean = true,
  temporality:  Boolean = true,
  monotonicity: Boolean = true
)
