package tps

import Graphs._

import tps.util.StringUtils

case class Profile(id: String, values: Seq[Option[Double]])

case class TimeSeries(
  labels: Seq[String], 
  profiles: Set[Profile]
) {
  // Internal map for fast access
  private val profileMap: Map[String, Profile] = profiles.map{
    case p @ Profile(id, vs) => id -> p
  }.toMap

  def profileByID(id: String): Option[Profile] = profileMap.get(id)

  def nbMeasurements: Int = labels.size
}

