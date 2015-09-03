package tps.util

class UniqueCounter {
  private var cnt = -1

  def next: Int = {
    cnt += 1
    cnt
  }
}
