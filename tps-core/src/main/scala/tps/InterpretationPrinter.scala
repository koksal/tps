package tps

import tps.synthesis.TriggerInterpretation

object InterpretationPrinter {
  def print(ti: TriggerInterpretation): String = {
    val ts = ti.timeSeries
    val ps = ts.profiles
    val n = ts.nbMeasurements

    val allowedAct = ti.allowedActivationIntervals
    val allowedInh = ti.allowedInhibitionIntervals

    val fields = "profile" +: ts.labels
    val tuples = ps.map{ p =>
      val pAct = allowedAct(p)
      val pInh = allowedInh(p)
      val values = for (i <- 0 until n) yield {
        val iAct = pAct contains i
        val iInh = pInh contains i
        if (iAct && iInh) "ambiguous"
        else if (iAct) "activation"
        else if (iInh) "inhibition"
        else "inactive"
      }
      p.id +: values
    }

    val tabular = TabularData(fields, tuples)
    tabular.toTSVString()
  }
}
