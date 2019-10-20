package circuitbreaker

import CircuitState.isWaitDurationOver

final case class CircuitState(status: CircuitStatus, resultBuffer: ResultBuffer[Boolean]) {

  def getStatus(implicit circuitBreakerConfig: CircuitBreakerConfig): CircuitState = {
    status match {
      case Open(startTime) =>
        if(isWaitDurationOver(startTime)) CircuitState(Closed, ResultBuffer.empty(circuitBreakerConfig.windowSize)) else CircuitState(Open(startTime),resultBuffer)
      case Closed =>
        if(resultBuffer.size >= circuitBreakerConfig.minimumNumberOfCalls && resultBuffer.matchPercentage(_ == false) >= circuitBreakerConfig.failureRateThreshold)
          CircuitState(Open(System.currentTimeMillis()), resultBuffer)
        else
          CircuitState(Closed, resultBuffer)
    }

  }

  def success: CircuitState = CircuitState(status, resultBuffer.push(true))

  def failure: CircuitState = CircuitState(status, resultBuffer.push(false))
}

object CircuitState {

  def initial(implicit config: CircuitBreakerConfig): CircuitState = CircuitState(Closed, ResultBuffer.empty(config.windowSize))

  def isWaitDurationOver(startTime: Long)(implicit config: CircuitBreakerConfig): Boolean =
    ((System.currentTimeMillis()-startTime) > (config.waitDurationInOpenState.toMillis))
}
