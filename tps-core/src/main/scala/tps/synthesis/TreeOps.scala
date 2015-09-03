package tps.synthesis

import Trees._

object TreeOps {
  def onlyOne(vs: Expr*): Expr = {
    val disjuncts = for (v <- vs) yield {
      val others = vs filter (_ != v)
      val notOthers = others map (o => Not(o))
      val onlyThis = And(v, And(notOthers: _*))
      onlyThis
    }
    Or(disjuncts: _*)
  }

  def atMostOne(vs: Expr*): Expr = {
    val conjs = for (v <- vs) yield {
      val others = vs filter (_ != v)
      val notOthers = others map (o => Not(o))
      val ifThisNotOthers = Implies(v, And(notOthers: _*))
      ifThisNotOthers
    }
    And(conjs: _*)
  }
}
