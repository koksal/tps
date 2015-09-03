package tps.util

object CollectionUtils {
  def pairAllToSuccessors[A](xs: List[A]): List[(A, A)] = xs match {
    case Nil => Nil
    case List(x) => Nil
    case x :: xs => xs.map((x, _)) ::: pairAllToSuccessors(xs)
  }
}
