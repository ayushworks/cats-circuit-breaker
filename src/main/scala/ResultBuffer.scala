package circuitbreaker

import scala.collection.immutable.Queue


final case class ResultBuffer[A] private (capacity: Int, size: Int, queue: Queue[A]) {
  def push(a: A): ResultBuffer[A] =
    if (size < capacity)
      ResultBuffer(capacity, size + 1, queue.enqueue(a))
    else
      queue.dequeue match {
        case (_, t) => ResultBuffer(capacity, size, t.enqueue(a))
      }

  def matchPercentage(predicate: A => Boolean): Double  =
    if(queue.size==0) 0 else (queue.filter(predicate).size.toDouble/queue.size.toDouble)*100
}

object ResultBuffer {

  def empty[A](capacity: Int): ResultBuffer[A] = ResultBuffer(capacity, 0, Queue.empty[A])
}
