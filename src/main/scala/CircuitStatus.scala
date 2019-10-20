package circuitbreaker

sealed trait CircuitStatus

case class Open(startTime: Long) extends CircuitStatus

case object Closed extends CircuitStatus