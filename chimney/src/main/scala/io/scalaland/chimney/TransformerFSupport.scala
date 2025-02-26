package io.scalaland.chimney

import scala.collection.compat.*

/** Type class supporting lifted transformers.
  *
  * In order to create lifted transformation from `A` to `F[B]`,
  * we need these few operations to be implemented for specific `F`
  * wrapper type.
  *
  * @deprecated migration described at [[https://scalalandio.github.io/chimney/partial-transformers/migrating-from-lifted.html]]
  *
  * @see [[TransformerFSupport.TransformerFOptionSupport]] for implementation for `Option`
  * @see [[TransformerFSupport.TransformerFEitherErrorAccumulatingSupport]] for implementation for `Either`
  *
  * @tparam F wrapper type constructor
  *
  * @since 0.5.0
  */
@deprecated("Lifted transformers are deprecated. Consider using PartialTransformer.", since = "Chimney 0.7.0")
trait TransformerFSupport[F[+_]] {

  /** Wrap a value into the type constructor `F`.
    *
    * @tparam A type of value
    * @param value value to wrap
    * @return wrapped value
    *
    * @since 0.5.0
    */
  def pure[A](value: A): F[A]

  /** Combine two wrapped values into wrapped pair of them.
    *
    * This method allows to decide on error handling semantics for
    * given type `F`.
    *
    * @tparam A type of first value
    * @tparam B type of second value
    * @param fa first wrapped value
    * @param fb second wrapped value
    * @return wrapped pair of values
    *
    * @since 0.5.0
    */
  def product[A, B](fa: F[A], fb: => F[B]): F[(A, B)]

  /** Transform wrapped value with given function.
    *
    * @tparam A type of wrapped value
    * @tparam B result type of provided function `f`
    * @param fa wrapped value
    * @param f function
    * @return wrapped result of function `f` applied to un
    *
    * @since 0.5.0
    */
  def map[A, B](fa: F[A], f: A => B): F[B]

  /** Perform traversal of function `f` on provided iterator of elements.
    *
    * Primarily used to perform recursive lifted transformation (given as function `f`) on a collection
    * type (Array, Seq, List, Vector, Map, etc.) for which we can obtain an `Iterator[A]`.
    *
    * This method allows to decide on error handling semantics for given type `F`, when transforming
    * between collections.
    *
    * @tparam M  type of collection where transformed elements are stored; note that this is not
    *            a type constructor, but a type with applied argument, so it can be List[B], Map[K, V], etc.
    * @tparam A  type of elements being iterated
    * @tparam B  target element type of function `f`
    * @param it  iterator of elements of type `A`
    * @param f   function to apply to elements of type `A`, returning `F[B]`
    * @param fac factory for collection type `M`
    * @return wrapped collection of type `F[M]`
    *
    * @since 0.5.0
    */
  def traverse[M, A, B](it: Iterator[A], f: A => F[B])(implicit
      fac: Factory[B, M]
  ): F[M]
}

/** @since 0.5.0 */
@deprecated("Lifted transformers are deprecated. Consider using PartialTransformer.", since = "Chimney 0.7.0")
object TransformerFSupport {

  /** `TransformerFSupport` instance for `Option`
    *
    * @since 0.5.0
    */
  implicit def TransformerFOptionSupport: TransformerFSupport[Option] =
    new TransformerFSupport[Option] {

      def pure[A](value: A): Option[A] = Option(value)

      def product[A, B](fa: Option[A], fb: => Option[B]): Option[(A, B)] = {
        fa.flatMap(a => fb.map(b => (a, b)))
      }

      def map[A, B](fa: Option[A], f: A => B): Option[B] = {
        fa.map(f)
      }

      def traverse[M, A, B](it: Iterator[A], f: A => Option[B])(implicit
          fac: Factory[B, M]
      ): Option[M] = {
        val b = fac.newBuilder
        var wasNone = false
        while (!wasNone && it.hasNext) {
          f(it.next()) match {
            case None     => wasNone = true
            case Some(fb) => b += fb
          }
        }
        if (wasNone) None else Some(b.result())
      }
    }

  /** `TransformerFSupport` instance for `Either[C[E], +*]`.
    *
    * @tparam E error type
    * @tparam C error accumulator type constructor
    * @param ef factory for error accumulator collection
    *
    * @since 0.5.0
    */
  implicit def TransformerFEitherErrorAccumulatingSupport[E, C[X] <: IterableOnce[X]](implicit
      ef: Factory[E, C[E]]
  ): TransformerFSupport[Either[C[E], +*]] = new TransformerFSupport[Either[C[E], +*]] {

    def pure[A](value: A): Either[C[E], A] = Right(value)

    def product[A, B](fa: Either[C[E], A], fb: => Either[C[E], B]): Either[C[E], (A, B)] = {
      (fa, fb) match {
        case (Right(ok1), Right(ok2)) =>
          Right((ok1, ok2))
        case (left1, left2) =>
          val eb = ef.newBuilder
          left1.left.foreach(eb ++= _)
          left2.left.foreach(eb ++= _)
          Left(eb.result())
      }
    }

    def map[A, B](fa: Either[C[E], A], f: A => B): Either[C[E], B] = {
      fa match {
        case Right(a)   => Right(f(a))
        case Left(errs) => Left(errs)
      }
    }

    def traverse[M, A, B](it: Iterator[A], f: A => Either[C[E], B])(implicit
        fac: Factory[B, M]
    ): Either[C[E], M] = {
      val bs = fac.newBuilder
      val eb = ef.newBuilder
      var hasErr = false

      while (it.hasNext) {
        f(it.next()) match {
          case Left(errs) =>
            eb ++= errs
            if (!hasErr) {
              hasErr = true
              bs.clear()
            }
          case Right(b) =>
            if (!hasErr) {
              bs += b
            }
        }
      }

      if (!hasErr) Right(bs.result()) else Left(eb.result())
    }
  }
}
