package circuitbreaker

import cats.effect.ExitCase.Completed
import cats.effect.concurrent.Ref
import cats.effect.Sync
import cats.implicits._
import scala.util.{Failure, Success, Try}
import scala.language.higherKinds

trait CircuitBreaker[F[_]] {

  def run[A](body: => A): F[A]

  def runF[A](fa: F[A]): F[A]
}

object CircuitBreaker {

  case class CircuitOpenException(msg:String) extends Exception(msg)

  def create[F[_]: Sync](implicit config: CircuitBreakerConfig): F[CircuitBreaker[F]] =
    Ref[F].of[CircuitState](CircuitState.initial).map {
      state =>
        new CircuitBreaker[F] {
          override def run[A](body: => A): F[A] = state.modify {
            st =>
              st.getStatus match {
                case CircuitState(Open(startTime), resultBuffer) => (CircuitState(Open(startTime), resultBuffer), Sync[F].raiseError[A](CircuitOpenException("circuit breaker open")))
                case state: CircuitState =>  Try(body) match {
                  case Success(value) => (state.success, Sync[F].pure(value))
                  case Failure(exception) => (state.failure, Sync[F].raiseError[A](exception))
                }
              }
          }.flatten

          override def runF[A](fa: F[A]): F[A] = state.modify {
            st =>
              st.getStatus match {
                case CircuitState(Open(startTime), resultBuffer) =>
                  (CircuitState(Open(startTime), resultBuffer), Sync[F].raiseError[A](CircuitOpenException("circuit breaker open")))
                case nextState: CircuitState =>
                  (nextState, Sync[F].guaranteeCase(fa){
                    case Completed =>
                      state.set(nextState.success)
                    case _ =>
                      state.set(nextState.failure)
                  })
              }
          }.flatten
        }
    }
}