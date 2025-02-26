package io.scalaland.chimney.utils

object EitherUtils {

  implicit final class OptionOps[T](private val opt: Option[T]) extends AnyVal {
    def toEither(err: => String): Either[String, T] = {
      opt match {
        case Some(value) => Right(value)
        case None        => Left(err)
      }
    }

    def toEitherList(err: => String): Either[List[String], T] = {
      opt match {
        case Some(value) => Right(value)
        case None        => Left(List(err))
      }
    }
  }

}
