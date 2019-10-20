import cats.MonadError
import cats.effect.Sync
import scala.language.higherKinds

package object circuitbreaker {

  def protect[F[_], A](body: => A, circuitBreaker: CircuitBreaker[F])(implicit ev: MonadError[F, Throwable]): F[A] =
    circuitBreaker.run(body)

  def protectF[F[_], A](fa: F[A], circuitBreaker: CircuitBreaker[F])(implicit ev: MonadError[F, Throwable], sy: Sync[F]): F[A] =
    circuitBreaker.runF(fa)
}