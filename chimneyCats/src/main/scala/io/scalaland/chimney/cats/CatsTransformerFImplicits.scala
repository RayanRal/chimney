package io.scalaland.chimney.cats

import _root_.cats.data.*
import _root_.cats.kernel.Semigroup
import _root_.cats.{Applicative, ApplicativeError}
import io.scalaland.chimney.*

import scala.collection.compat.*

/** @since 0.5.0 */
trait CatsTransformerFImplicits extends LowPriorityCatsTransformerFImplicits {

  // normally, these instances are not needed, but few tests with .enableUnsafeOption fail to compile without them
  /** @since 0.5.0 */
  implicit def TransformerFValidatedNecSupport[E]: TransformerFSupport[ValidatedNec[E, +*]] =
    TransformerFValidatedSupport[NonEmptyChain[E]](implicitly)

  /** @since 0.5.0 */
  implicit def TransformerFValidatedNelSupport[E]: TransformerFSupport[ValidatedNel[E, +*]] =
    TransformerFValidatedSupport[NonEmptyList[E]](implicitly)

  /** @since 0.5.3 */
  implicit def TransformerFIorNecSupport[E]: TransformerFSupport[IorNec[E, +*]] =
    TransformerFIorSupport[NonEmptyChain[E]](implicitly)

  /** @since 0.5.3 */
  implicit def TransformerFIorNelSupport[E]: TransformerFSupport[IorNel[E, +*]] =
    TransformerFIorSupport[NonEmptyList[E]](implicitly)

  /** @since 0.5.3 */
  implicit def TransformerFIorNesSupport[E]: TransformerFSupport[IorNes[E, +*]] =
    TransformerFIorSupport[NonEmptySet[E]](implicitly)

  /** @since 0.6.3 */
  implicit def TransformerFErrorValidatedNecSupport[M]
      : TransformerFErrorPathSupport[ValidatedNec[TransformationError[M], +*]] =
    TransformerFValidatedErrorPathSupport[ValidatedNec[TransformationError[M], +*], NonEmptyChain, M]

  /** @since 0.6.3 */
  implicit def TransformerFErrorValidatedNelSupport[M]
      : TransformerFErrorPathSupport[ValidatedNel[TransformationError[M], +*]] =
    TransformerFValidatedErrorPathSupport[ValidatedNel[TransformationError[M], +*], NonEmptyList, M]

  /** @since 0.6.3 */
  implicit def TransformerFErrorIorNecSupport[M]: TransformerFErrorPathSupport[IorNec[TransformationError[M], +*]] =
    TransformerFValidatedErrorPathSupport[IorNec[TransformationError[M], +*], NonEmptyChain, M]

  /** @since 0.6.3 */
  implicit def TransformerFErrorIorNelSupport[M]: TransformerFErrorPathSupport[IorNel[TransformationError[M], +*]] =
    TransformerFValidatedErrorPathSupport[IorNel[TransformationError[M], +*], NonEmptyList, M]
}

trait LowPriorityCatsTransformerFImplicits {

  /** @since 0.5.3 */
  implicit def TransformerFIorSupport[EE: Semigroup]: TransformerFSupport[Ior[EE, +*]] =
    new TransformerFSupport[Ior[EE, +*]] {
      override def pure[A](value: A): Ior[EE, A] =
        Ior.right(value)

      override def product[A, B](fa: Ior[EE, A], fb: => Ior[EE, B]): Ior[EE, (A, B)] =
        fa.flatMap(a => fb.map(b => (a, b)))

      override def map[A, B](fa: Ior[EE, A], f: A => B): Ior[EE, B] =
        fa.map(f)

      override def traverse[M, A, B](it: Iterator[A], f: A => Ior[EE, B])(implicit fac: Factory[B, M]): Ior[EE, M] = {
        var leftSide: EE = null.asInstanceOf[EE]
        val rightSide = fac.newBuilder
        var isLeft = false

        while (it.hasNext && !isLeft) {
          f(it.next()) match {
            case Ior.Left(a) =>
              if (leftSide == null) leftSide = a
              else leftSide = Semigroup[EE].combine(leftSide, a)
              isLeft = true

            case Ior.Right(b) =>
              rightSide += b

            case Ior.Both(a, b) =>
              if (leftSide == null) leftSide = a
              else leftSide = Semigroup[EE].combine(leftSide, a)
              rightSide += b
          }
        }

        if (isLeft) Ior.left(leftSide)
        else if (leftSide != null) Ior.both(leftSide, rightSide.result())
        else Ior.right(rightSide.result())
      }
    }

  /** @since 0.5.0 */
  implicit def TransformerFValidatedSupport[EE: Semigroup]: TransformerFSupport[Validated[EE, +*]] =
    new TransformerFSupport[Validated[EE, +*]] {
      def pure[A](value: A): Validated[EE, A] =
        Validated.Valid(value)

      def product[A, B](fa: Validated[EE, A], fb: => Validated[EE, B]): Validated[EE, (A, B)] =
        fa.product(fb)

      def map[A, B](fa: Validated[EE, A], f: A => B): Validated[EE, B] =
        fa.map(f)

      def traverse[M, A, B](it: Iterator[A], f: A => Validated[EE, B])(implicit
          fac: Factory[B, M]
      ): Validated[EE, M] = {
        val (valid, invalid) = it.map(f).partition(_.isValid)
        Semigroup[EE].combineAllOption(invalid.collect { case Validated.Invalid(e) => e }) match {
          case Some(errors) =>
            Validated.Invalid(errors)
          case None =>
            val b = fac.newBuilder
            b ++= valid.collect { case Validated.Valid(b) => b }
            Validated.Valid(b.result())
        }
      }
    }

  /** @since 0.6.1 */
  implicit def TransformerFValidatedErrorPathSupport[F[+_], EE[_]: Applicative, M](implicit
      applicativeError: ApplicativeError[F, EE[TransformationError[M]]]
  ): TransformerFErrorPathSupport[F] =
    new TransformerFErrorPathSupport[F] {
      override def addPath[A](fa: F[A], node: ErrorPathNode): F[A] =
        applicativeError.handleErrorWith(fa)(ee =>
          applicativeError.raiseError(Applicative[EE].map(ee)(_.prepend(node)))
        )
    }
}
