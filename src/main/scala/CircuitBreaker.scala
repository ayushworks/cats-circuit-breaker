package circuitbreaker

import cats.MonadError
import cats.effect.ExitCase.Completed
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import scala.util.{Failure, Success, Try}
import scala.language.higherKinds

trait CircuitBreaker[F[_]] {

  def run[A](body: => A)(implicit ev: MonadError[F, Throwable]): F[A]

  def runF[A](fa: F[A])(implicit ev: MonadError[F, Throwable], sy: Sync[F]): F[A]
}

object CircuitBreaker {

  case class CircuitOpenException(msg:String) extends Exception(msg)

  def create[F[_]: Concurrent](implicit config: CircuitBreakerConfig): F[CircuitBreaker[F]] =
    Ref[F].of[CircuitState](CircuitState.initial).map {
      state =>
        new CircuitBreaker[F] {
          override def run[A](body: => A)(implicit ev: MonadError[F, Throwable]): F[A] = state.modify {
            st =>
              st.getStatus match {
                case CircuitState(Open(startTime), resultBuffer) => (CircuitState(Open(startTime), resultBuffer), ev.raiseError[A](CircuitOpenException("circuit breaker open")))
                case state: CircuitState =>  Try(body) match {
                  case Success(value) => (state.success, ev.pure(value))
                  case Failure(exception) => (state.failure, ev.raiseError[A](exception))
                }
              }
          }.flatten

          override def runF[A](fa: F[A])(implicit ev: MonadError[F, Throwable], sy: Sync[F]): F[A] = state.modify {
            st =>
              st.getStatus match {
                case CircuitState(Open(startTime), resultBuffer) =>
                  (CircuitState(Open(startTime), resultBuffer), ev.raiseError[A](CircuitOpenException("circuit breaker open")))
                case nextState: CircuitState =>
                  (nextState, sy.guaranteeCase(fa){
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