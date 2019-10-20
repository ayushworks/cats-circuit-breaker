package circuitbreaker

import scala.concurrent.duration.Duration
import scala.concurrent.duration._


/**
  *
  * @param failureRateThreshold : Configures the failure rate threshold in percentage. When the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open and starts short-circuiting calls.
  * @param windowSize : Configures the size of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed.
  * @param minimumNumberOfCalls: Configures the minimum number of calls which are required (per sliding window period) before the CircuitBreaker can calculate the error rate.
  * @param waitDurationInOpenState: The duration that the CircuitBreaker should wait before transitioning from open to closed.
  */
final case class CircuitBreakerConfig(failureRateThreshold: Double, windowSize: Int, minimumNumberOfCalls: Int, waitDurationInOpenState: Duration)

object CircuitBreakerConfig {

  def defaultConfig: CircuitBreakerConfig = CircuitBreakerConfig(50, 100, 10, 60 seconds)
}
