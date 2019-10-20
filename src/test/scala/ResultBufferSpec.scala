import circuitbreaker.ResultBuffer
import org.scalatest.{FlatSpec, Matchers}

/**
 * @author Ayush Mittal
 */

class ResultBufferSpec extends FlatSpec with Matchers {

  "ResultBuffer" should "push data when queue is not full" in {
    val resultBuffer = ResultBuffer.empty[Int](3)
    resultBuffer.push(1).push(2).push(3).size shouldBe 3
    resultBuffer.push(1).push(2).push(3).queue.dequeue._1 shouldBe 1
  }

  "ResultBuffer" should "pop last and push new when queue is full" in {
    val resultBuffer = ResultBuffer.empty[Int](3)
    val buffer = resultBuffer.push(1).push(2).push(3)
    buffer.push(4).size shouldBe 3
    buffer.push(4).queue.dequeue._1 shouldBe 2
  }

  "ResultBuffer" should "match percentage for predicate" in {
    val resultBuffer = ResultBuffer.empty[Boolean](2)
    val buffer = resultBuffer.push(true).push(false)
    buffer.matchPercentage(_ == true) shouldBe 50
  }
}
