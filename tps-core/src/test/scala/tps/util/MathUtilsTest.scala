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

  test("mean of list") {
    val l1 = List(1.0)
    MathUtils.mean(l1) should equal (1.0)

    val l2 = List(8.0, 2.0, 6.0, 4.0)
    MathUtils.mean(l2) should equal (5.0)
  }

  test("median of list") {
    val l1 = List(1.0)
    MathUtils.median(l1) should equal (1.0)

    val l2 = List(8.0, 2.0, 6.0, 4.0)
    MathUtils.median(l2) should equal (5.0)
  }
}
