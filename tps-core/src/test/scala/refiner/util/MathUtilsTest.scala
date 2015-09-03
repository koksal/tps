package tps.util
package test

import org.scalatest.FunSuite
import org.scalatest.Matchers

class MathUtilsTest extends FunSuite with Matchers {
  test("two-element combinations of three elements") {
    val list = List(1, 2, 3)
    val comb = MathUtils.combination(list, 2)
    val expected = Set(
      List(1, 2),
      List(2, 3),
      List(1, 3)
    )
    comb.toSet should equal (expected)
  }

  test("no four-element combinations of three elements") {
    val list = List(1, 2, 3)
    val c = MathUtils.combination(list, 4)
    c should equal (Nil)
  }

  test("one zero-elt combination") {
    val list = List(1, 2, 3)
    val c = MathUtils.combination(list, 0)
    c should equal (List(Nil))
  }
}
