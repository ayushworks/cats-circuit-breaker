# cats-circuit-breaker

[![Build Status](https://travis-ci.com/ayushworks/pariksha.svg?branch=master)](https://travis-ci.com/ayushworks/pariksha)

**cats-circuit-breaker** is a purely functional circuit breaker. It
allows you to decorate services with a circuit breaker. 

### Design principles

**cats-circuit-breaker** is an count based circuit breaker. 
The core idea is to wrap a protected function call in a circuit breaker, 
which monitors for failures. When the failures reach a certain _threshold_, 
the circuit breaker trips, and all further calls to the circuit breaker return with an error upto a _waitDuration_.

For now **failures** is same as exceptions. This could be optimised in future to allow you to define what
a failure is.

The library uses types 
from [cats-effect](https://typelevel.org/cats-effect/concurrency/) for managing asynchronous, concurrent mutable references.

**cats-circuit-breaker** is completely pure, which allows for ease of reasoning
and composability. The library is inspired by *SystemFw's* [Upperbound](https://github.com/SystemFw/upperbound) library and by his [talks](http://systemfw.org/talks.html) on how 
to manage shared state in pure FP. 

### Usage

##### CircuitBreaker

The protagonist of the library is a `CircuitBreaker`, which is defined as:

``` scala
trait CircuitBreaker[F[_]] {

  def run[A](body: => A): F[A]

  def runF[A](fa: F[A]): F[A]
}
```

The `run` method takes a thunk of `A`, which can represent any
program, and returns an `F[A]` that represents the action of
wrapping it within the circuit breaker. 

The `runF` method takes an `F[A]`, which can represent any
program, and returns an `F[A]` that represents the action of
wrapping it within the circuit breaker. 


The `CircuitBreaker` trait is the core component of the library and allows you to
protect a service from being overloaded in case of recurring failures.

##### CircuitBreakerConfig
```scala
case class CircuitBreakerConfig(failureRateThreshold: Double, windowSize: Int, minimumNumberOfCalls: Int, waitDurationInOpenState: Duration)
```

* failureRateThreshold : the failure rate threshold in percentage. Circuit breaker is open when this rate is exceeded.
* windowSize: the size of the window which is used to monitor the outcome of underlying service when circuit breaker is closed.
* minimumNumberOfCalls: minimum number of calls which are required (per sliding window period) before the CircuitBreaker can calculate the error rate.
* waitDurationInOpenState: The time that the CircuitBreaker should wait before transitioning from open to closed.

##### Creating a CircuitBreaker

To create a `CircuitBreaker`, use the `create` method:

``` scala
object CircuitBreaker {
  def create[F[_]: Concurrent](implicit config: CircuitBreakerConfig): F[CircuitBreaker[F]]
}
```

`create` creates a new `CircuitBreaker`.


##### Wrapping underlying services 

The underlying service which is being protected could be in two flavours - embellished or bare.

* protecting an embellished service requires us to use `protectF` method

```scala

def getData[F[_]](id: Int): F[User]

def protectedGetData[F](id: Int): F[User] = {
  import circuitbreaker.protectF 
  protectF(getData(id), circuitBreaker)
}
    

```

* protecting a bare service requires us to use `protect` method

```scala

def getData(id: Int): User

def protectedGetData[F](id: Int)(circuitBreaker: CircuitBreaker[F])(implicit ev: MonadError[F, Throwable]): F[User] = {
  import circuitbreaker.protect 
  protect(getData(id), circuitBreaker)
}
    

```

##### Using the same breaker for multiple services

We can also use the same circuit breaker for monitoring multiple services if required.

```scala
    
    for {
      breaker <- CircuitBreaker.create(CircuitBreakerConfig.defaultConfig)
      user <- breaker.run(userService.getUser(id))
      socialInfo <- breaker.run(socialService.getInfo(user))
      ..
    }
```

In the above example , the `userService` and `socialService` calls are monitored by the same instance of 
circuit breaker.

### Whats next 

* Add support for events on circuit breaker state changes
* Allow you to configure what constitues a failure
* Provide support for quality based circuit state eg..slow response times
* Allow ignorable exceptions