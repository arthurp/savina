package edu.rice.habanero.benchmarks.quicksort

import java.util

import edu.rice.habanero.actors.HabaneroSelector
import edu.rice.habanero.benchmarks.{Benchmark, BenchmarkRunner}
import edu.rice.hj.Module0._
import edu.rice.hj.api.HjSuspendable

/**
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
object QuickSortHabaneroSelectorBenchmark {

  def main(args: Array[String]) {
    BenchmarkRunner.runBenchmark(args, new QuickSortHabaneroSelectorBenchmark)
  }

  private final class QuickSortHabaneroSelectorBenchmark extends Benchmark {
    def initialize(args: Array[String]) {
      QuickSortConfig.parseArgs(args)
    }

    def printArgInfo() {
      QuickSortConfig.printArgs()
    }

    def runIteration() {
      finish(new HjSuspendable {
        override def run() = {

          val input = QuickSortConfig.randomlyInitArray()

          val rootActor = new QuickSortActor(null, PositionInitial)
          rootActor.start()
          rootActor.send(0, SortMessage(input))
        }
      })
    }

    def cleanupIteration(lastIteration: Boolean, execTimeMillis: Double) {
    }
  }

  private abstract class Position

  private case object PositionRight extends Position

  private case object PositionLeft extends Position

  private case object PositionInitial extends Position

  private abstract class Message

  private case class SortMessage(data: java.util.List[java.lang.Long]) extends Message

  private case class ResultMessage(data: java.util.List[java.lang.Long], position: Position) extends Message

  private class QuickSortActor(parent: QuickSortActor, positionRelativeToParent: Position) extends HabaneroSelector[AnyRef](1) {

    private val selfActor = this
    private var result: java.util.List[java.lang.Long] = null
    private var numFragments = 0

    def notifyParentAndTerminate() {

      if (positionRelativeToParent eq PositionInitial) {
        QuickSortConfig.checkSorted(result)
      }
      if (parent ne null) {
        parent.send(0, ResultMessage(result, positionRelativeToParent))
      }
      exit()
    }

    override def process(msg: AnyRef) {
      msg match {
        case SortMessage(data) =>

          val dataLength: Int = data.size()
          if (dataLength < QuickSortConfig.T) {

            result = QuickSortConfig.quicksortSeq(data)
            notifyParentAndTerminate()

          } else {

            val dataLengthHalf = dataLength / 2
            val pivot = data.get(dataLengthHalf)

            val leftUnsorted = QuickSortConfig.filterLessThan(data, pivot)
            val leftActor = new QuickSortActor(selfActor, PositionLeft)
            leftActor.start()
            leftActor.send(0, SortMessage(leftUnsorted))

            val rightUnsorted = QuickSortConfig.filterGreaterThan(data, pivot)
            val rightActor = new QuickSortActor(selfActor, PositionRight)
            rightActor.start()
            rightActor.send(0, SortMessage(rightUnsorted))

            result = QuickSortConfig.filterEqualsTo(data, pivot)
            numFragments += 1
          }

        case ResultMessage(data, position) =>

          if (!data.isEmpty) {
            if (position eq PositionLeft) {
              val temp = new util.ArrayList[java.lang.Long]()
              temp.addAll(data)
              temp.addAll(result)
              result = temp
            } else if (position eq PositionRight) {
              val temp = new util.ArrayList[java.lang.Long]()
              temp.addAll(result)
              temp.addAll(data)
              result = temp
            }
          }

          numFragments += 1
          if (numFragments == 3) {
            notifyParentAndTerminate()
          }
      }
    }
  }

}