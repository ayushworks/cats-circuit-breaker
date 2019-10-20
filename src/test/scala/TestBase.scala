import cats.effect.{ContextShift, IO}
import circuitbreaker.{CircuitBreaker, CircuitBreakerConfig}
import circuitbreaker.protect
import circuitbreaker.protectF
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * @author Ayush Mittal
 */
trait TestBase {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  implicit val config = CircuitBreakerConfig.defaultConfig.copy(failureRateThreshold = 50, windowSize = 10, minimumNumberOfCalls = 1, waitDurationInOpenState = 1 second)

  implicit val timer = IO.timer(global)

  private def passingAlways(id: Int): String = id.toString

  private def passingSometimes(id: Int): String = {
    if(id==6) throw new RuntimeException("error")
    id.toString
  }

  private def halfFailing(id: Int): String = {
    if(id%2==0) throw new RuntimeException("error")
    id.toString
  }

  def passingAlways(id: Int, circuitBreaker: CircuitBreaker[IO]): IO[String]  =
    protect(passingAlways(id), circuitBreaker)

  def passingSometimes(id: Int, circuitBreaker: CircuitBreaker[IO]): IO[String]  =
    protect(passingSometimes(id), circuitBreaker).handleErrorWith(ex => IO(ex.getMessage))

  def halfFailing(id: Int, circuitBreaker: CircuitBreaker[IO]): IO[String]  =
    protect(halfFailing(id), circuitBreaker).handleErrorWith(ex => IO(ex.getMessage))

  def passingAlwaysF(id: Int)(circuitBreaker: CircuitBreaker[IO]): IO[String]  =
    protectF(IO(passingAlways(id)), circuitBreaker)

  def passingSometimesF(id: Int)(circuitBreaker: CircuitBreaker[IO]): IO[String]  =
    protectF(IO(passingSometimes(id)), circuitBreaker).handleErrorWith(ex => IO(ex.getMessage))

  def halfFailingF(id: Int)(circuitBreaker: CircuitBreaker[IO]): IO[String]  =
    protectF(IO(halfFailing(id)), circuitBreaker).handleErrorWith(ex => IO(ex.getMessage))

}

