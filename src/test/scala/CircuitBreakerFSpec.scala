import cats.effect.IO
import cats.implicits._
import circuitbreaker.CircuitBreaker
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

/**
 * @author Ayush Mittal
 */
class CircuitBreakerFSpec extends FlatSpec with Matchers with TestBase {


  "CircuitBreaker" should "pass successfully for all underlying successful calls" in {

    val allResults: IO[List[String]] = for {
      breaker <- CircuitBreaker.create[IO]
      run <- List.range(0,10).traverse(id => passingAlwaysF(id)(breaker))
    } yield run


    allResults.unsafeRunSync() shouldBe List.range(0,10).map(_.toString)

  }

  "CircuitBreaker" should "remain closed for failures within threshold" in {

    val allResults : IO[List[String]] = for {
      breaker <- CircuitBreaker.create[IO]
      run <- List.range(0,10).traverse(id => passingSometimesF(id)(breaker))
    } yield run

    allResults.unsafeRunSync() shouldBe List.range(0,6).map(_.toString)++List("error")++List.range(7,10).map(_.toString)

  }

  "CircuitBreaker" should "move to open state for failures beyond threshold" in {

    val allResults  = for {
      breaker <- CircuitBreaker.create[IO]
      run1 <- halfFailingF(1)(breaker)
      run2 <- halfFailingF(2)(breaker)
      run3 <- halfFailingF(3)(breaker)
      _ <- IO.sleep(1 second)
      run5 <- halfFailingF(5)(breaker)
      run6 <- halfFailingF(6)(breaker)
      run7 <- halfFailingF(7)(breaker)
      run8 <- halfFailingF(8)(breaker)
    } yield List(run1, run2, run3, run5, run6, run7, run8)

    allResults.unsafeRunSync() shouldBe List("1", "error", "circuit breaker open", "5", "error", "circuit breaker open", "circuit breaker open")

  }


}
