package tps.util

import org.scalatest.FunSuite

class CollectionUtilsTest extends FunSuite {
  test("permute a sequence") {
    val permutation = List(1, 0, 3, 2)
    val xs = List("a", "b", "c", "d")

    val expected = List("b", "a", "d", "c")
    assertResult(expected)(CollectionUtils.permuteSeq(xs, permutation))
  }
}
