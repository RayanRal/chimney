package io.scalaland.chimney

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.utils.OptionUtils.*
import utest.*

import scala.annotation.unused
import scala.collection.immutable.Queue
import scala.collection.mutable.ArrayBuffer

object PartialTransformerStdLibTypesSpec extends TestSuite {

  val tests = Tests {

    test("not support converting non-Unit field to Unit field if there is no implicit converter allowing that") {
      case class Buzz(value: String)
      case class ConflictingFooBuzz(value: Unit)

      compileError("""Buzz("a").transformIntoPartial[ConflictingFooBuzz]""").check(
        "",
        "Chimney can't derive transformation from Buzz to ConflictingFooBuzz",
        "io.scalaland.chimney.PartialTransformerStdLibTypesSpec.ConflictingFooBuzz",
        "value: scala.Unit - can't derive transformation from value: java.lang.String in source type io.scalaland.chimney.PartialTransformerStdLibTypesSpec.Buzz",
        "scala.Unit",
        "derivation from buzz.value: java.lang.String to scala.Unit is not supported in Chimney!",
        "Consult https://scalalandio.github.io/chimney for usage examples."
      )
    }

    test("support automatically filling of scala.Unit") {
      case class Buzz(value: String)
      case class NewBuzz(value: String, unit: Unit)
      case class FooBuzz(unit: Unit)
      case class ConflictingFooBuzz(value: Unit)

      Buzz("a").transformIntoPartial[NewBuzz].asOption ==> Some(NewBuzz("a", ()))
      Buzz("a").transformIntoPartial[FooBuzz].asOption ==> Some(FooBuzz(()))
      NewBuzz("a", null.asInstanceOf[Unit]).transformIntoPartial[FooBuzz].asOption ==> Some(
        FooBuzz(null.asInstanceOf[Unit])
      )
    }

    test("transform from Option-type into Option-type, using Total Transformer for inner type transformation") {

      implicit val intPrinter: Transformer[Int, String] = _.toString

      test("when inner value is non-empty") {
        val result = Option(123).transformIntoPartial[Option[String]]

        result.asOption ==> Some(Some("123"))
        result.asEither ==> Right(Some("123"))
        result.asErrorPathMessageStrings ==> Iterable.empty
      }

      test("when inner value is empty") {
        val result = Option.empty[Int].transformIntoPartial[Option[String]]

        result.asOption ==> Some(None)
        result.asEither ==> Right(None)
        result.asErrorPathMessageStrings ==> Iterable.empty
      }
    }

    test("transform from non-Option-type into Option-type, using Partial Transformer for inner type transformation") {

      implicit val intPartialParser: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResultOrString("bad int"))

      test("when Result is success") {
        val result = Option("123").transformIntoPartial[Option[Int]]

        result.asOption ==> Some(Some(123))
        result.asEither ==> Right(Some(123))
        result.asErrorPathMessageStrings ==> Iterable.empty
      }

      test("when Result is failure") {
        val result = Option("abc").transformIntoPartial[Option[Int]]
        result.asOption ==> None
        result.asEither ==> Left(
          partial.Result.Errors.fromString("bad int")
        )
        result.asErrorPathMessageStrings ==> Iterable(
          "" -> "bad int"
        )
      }

      test("when Result is null") {
        val result = Option.empty[String].transformIntoPartial[Option[Int]]

        result.asOption ==> Some(None)
        result.asEither ==> Right(None)
        result.asErrorPathMessageStrings ==> Iterable.empty
      }
    }

    test("transform from non-Option-type into Option-type, using Total Transformer for inner type transformation") {

      implicit val intPrinter: Transformer[Int, String] = _.toString

      test("when inner value is non-null") {
        val result = 10.transformIntoPartial[Option[String]]

        result.asOption ==> Some(Some("10"))
        result.asEither ==> Right(Some("10"))
        result.asErrorPathMessageStrings ==> Iterable.empty
      }

      test("when inner value is null") {
        implicit val integerPrinter: Transformer[Integer, String] = _.toString

        val result = (null: Integer).transformIntoPartial[Option[String]]

        result.asOption ==> Some(None)
        result.asEither ==> Right(None)
        result.asErrorPathMessageStrings ==> Iterable.empty
      }
    }

    test("transform from non-Option-type into Option-type, using Partial Transformer for inner type transformation") {

      implicit val intPartialParser: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResultOrString("bad int"))

      test("when Result is success") {
        val result = "123".transformIntoPartial[Option[Int]]

        result.asOption ==> Some(Some(123))
        result.asEither ==> Right(Some(123))
        result.asErrorPathMessageStrings ==> Iterable.empty
      }

      test("when Result is failure") {
        val result = "abc".transformIntoPartial[Option[Int]]
        result.asOption ==> None
        result.asEither ==> Left(
          partial.Result.Errors.fromString("bad int")
        )
        result.asErrorPathMessageStrings ==> Iterable(
          "" -> "bad int"
        )
      }

      test("when Result is null") {
        val result = (null: String).transformIntoPartial[Option[Int]]

        result.asOption ==> Some(None)
        result.asEither ==> Right(None)
        result.asErrorPathMessageStrings ==> Iterable.empty
      }
    }

    test("transform from Option-type into non-Option-type, using Total Transformer for inner type transformation") {

      implicit val intPrinter: Transformer[Int, String] = _.toString

      test("when option is non-empty") {
        val result = Option(10).transformIntoPartial[String]

        result.asOption ==> Some("10")
        result.asEither ==> Right("10")
        result.asErrorPathMessageStrings ==> Iterable.empty
      }

      test("when option is empty") {
        val result = Option.empty[Int].transformIntoPartial[String]

        result.asOption ==> None
        result.asEither ==> Left(partial.Result.fromEmpty)
        result.asErrorPathMessageStrings ==> Iterable(("", "empty value"))
      }
    }

    test("transform from Option-type into non-Option-type, using Partial Transformer for inner type transformation") {

      implicit val intPartialParser: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResultOrString("bad int"))

      test("when option is non-empty and inner is success") {
        val result = Option("10").transformIntoPartial[Int]

        result.asOption ==> Some(10)
        result.asEither ==> Right(10)
        result.asErrorPathMessageStrings ==> Iterable.empty
      }

      test("when option is non-empty and inner is failure") {
        val result = Some("abc").transformIntoPartial[Int]

        result.asOption ==> None
        result.asEither ==> Left(partial.Result.fromErrorString("bad int"))
        result.asErrorPathMessageStrings ==> Iterable("" -> "bad int")
      }

      test("when option is empty") {
        val result = (None: Option[String]).transformIntoPartial[Int]

        result.asOption ==> None
        result.asEither ==> Left(partial.Result.fromEmpty)
        result.asErrorPathMessageStrings ==> Iterable(("", "empty value"))
      }
    }

    test("transform from Either-type into Either-type, using Total Transformer for inner types transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      (Left(1): Either[Int, Int]).transformIntoPartial[Either[String, String]].asOption ==> Some(Left("1"))
      (Right(1): Either[Int, Int]).transformIntoPartial[Either[String, String]].asOption ==> Some(Right("1"))
      Left(1).transformIntoPartial[Either[String, String]].asOption ==> Some(Left("1"))
      Right(1).transformIntoPartial[Either[String, String]].asOption ==> Some(Right("1"))
      Left(1).transformIntoPartial[Left[String, String]].asOption ==> Some(Left("1"))
      Right(1).transformIntoPartial[Right[String, String]].asOption ==> Some(Right("1"))
    }

    test("transform from Either-type into Either-type, using Lifted Transformer for inner types transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      (Left("1"): Either[String, String]).transformIntoPartial[Either[Int, Int]].asOption ==> Some(Left(1))
      (Right("1"): Either[String, String]).transformIntoPartial[Either[Int, Int]].asOption ==> Some(Right(1))
      Left("1").transformIntoPartial[Either[Int, Int]].asOption ==> Some(Left(1))
      Right("1").transformIntoPartial[Either[Int, Int]].asOption ==> Some(Right(1))
      Left("1").transformIntoPartial[Left[Int, Int]].asOption ==> Some(Left(1))
      Right("1").transformIntoPartial[Right[Int, Int]].asOption ==> Some(Right(1))

      (Left("x"): Either[String, String]).transformIntoPartial[Either[Int, Int]].asOption ==> None
      (Right("x"): Either[String, String]).transformIntoPartial[Either[Int, Int]].asOption ==> None
      Left("x").transformIntoPartial[Either[Int, Int]].asOption ==> None
      Right("x").transformIntoPartial[Either[Int, Int]].asOption ==> None
      Left("x").transformIntoPartial[Left[Int, Int]].asOption ==> None
      Right("x").transformIntoPartial[Right[Int, Int]].asOption ==> None
    }

    test("transform Iterable-type to Iterable-type, using Total Transformer for inner type transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      List(123, 456).transformIntoPartial[Vector[String]].asOption ==> Some(Vector("123", "456"))
      Vector(123, 456).transformIntoPartial[Queue[String]].asOption ==> Some(Queue("123", "456"))
      Queue(123, 456).transformIntoPartial[List[String]].asOption ==> Some(List("123", "456"))
    }

    test("transform Iterable-type to Iterable-type, using Partial Transformer for inner type transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      List("123", "456").transformIntoPartial[List[Int]].asOption ==> Some(Vector(123, 456))
      Vector("123", "456").transformIntoPartial[Queue[Int]].asOption ==> Some(Queue(123, 456))
      Queue("123", "456").transformIntoPartial[List[Int]].asOption ==> Some(List(123, 456))

      List("abc", "456").transformIntoPartial[Vector[Int]].asOption ==> None
      Vector("123", "def").transformIntoPartial[Queue[Int]].asOption ==> None
      Queue("123", "def").transformIntoPartial[List[Int]].asOption ==> None

      List("abc", "456", "ghi")
        .transformIntoPartial[Vector[Int]](failFast = false)
        .asErrorPathMessageStrings ==> Iterable("(0)" -> "empty value", "(2)" -> "empty value")
      Vector("123", "def", "ghi")
        .transformIntoPartial[Queue[Int]](failFast = false)
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value", "(2)" -> "empty value")
      Queue("123", "def", "ghi")
        .transformIntoPartial[List[Int]](failFast = false)
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value", "(2)" -> "empty value")

      List("abc", "456", "ghi")
        .transformIntoPartial[Vector[Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(0)" -> "empty value")
      Vector("123", "def", "ghi")
        .transformIntoPartial[Queue[Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value")
      Queue("123", "def", "ghi")
        .transformIntoPartial[List[Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value")
    }

    test("transform Array-type to Array-type, using Total Transformer for inner type transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      Array(123, 456).transformIntoPartial[Array[String]].asOption.get ==> Array("123", "456")
    }

    test("transform Array-type to Array-type, using Partial Transformer for inner type transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      Array("123", "456").transformIntoPartial[Array[Int]].asOption.get ==> Array(123, 456)
      Array("abc", "456").transformIntoPartial[Array[Int]].asOption ==> None

      Array("abc", "456", "ghi")
        .transformIntoPartial[Array[Int]]
        .asErrorPathMessageStrings ==> Iterable("(0)" -> "empty value", "(2)" -> "empty value")

      Array("abc", "456", "ghi")
        .transformIntoPartial[Array[Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(0)" -> "empty value")
    }

    test("transform between Array-type and Iterable-type, using Total Transformer for inner type transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      Array(123, 456).transformIntoPartial[Set[String]].asOption ==> Some(Set("123", "456"))
      Array.empty[Int].transformIntoPartial[Set[String]].asOption ==> Some(Set.empty[String])
    }

    test("transform between Array-type and Iterable-type, using Partial Transformer for inner type transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      Set("123", "456").transformIntoPartial[Array[Int]].asOption.get.sorted ==> Array(123, 456)
      Set("123", "xyz").transformIntoPartial[Array[Int]].asOption ==> None
      Set.empty[String].transformIntoPartial[Array[Int]].asOption.get ==> Array.empty[String]

      Array("123", "456").transformIntoPartial[Set[Int]].asOption ==> Some(Set(123, 456))
      Array("123", "xyz").transformIntoPartial[Set[Int]].asOption ==> None
      Array.empty[String].transformIntoPartial[Set[Int]].asOption ==> Some(Set.empty[Int])

      Array("123", "xyz", "ghi")
        .transformIntoPartial[Set[Int]]
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value", "(2)" -> "empty value")

      Array("123", "xyz", "ghi")
        .transformIntoPartial[Set[Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value")
    }

    test("transform Map-type to Map-type, using Total Transformer for inner type transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      Map(1 -> 10, 2 -> 20).transformIntoPartial[Map[String, String]].asOption ==> Some(Map("1" -> "10", "2" -> "20"))
      Map(1 -> 10, 2 -> 20).transformIntoPartial[Map[String, Int]].asOption ==> Some(Map("1" -> 10, "2" -> 20))
    }

    test("transform Map-type to Map-type, using Partial Transformer for inner type transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      Map("1" -> "10", "2" -> "20").transformIntoPartial[Map[Int, Int]].asOption ==> Some(Map(1 -> 10, 2 -> 20))
      Map("1" -> "10", "2" -> "20").transformIntoPartial[Map[Int, String]].asOption ==> Some(
        Map(1 -> "10", 2 -> "20")
      )

      Map("1" -> "x", "y" -> "20").transformIntoPartial[Map[Int, Int]].asOption ==> None
      Map("x" -> "10", "2" -> "20").transformIntoPartial[Map[Int, String]].asOption ==> None

      Map("1" -> "x", "y" -> "20")
        .transformIntoPartial[Map[Int, Int]]
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value", "keys(y)" -> "empty value")

      Map("1" -> "x", "y" -> "20")
        .transformIntoPartial[Map[Int, Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(1)" -> "empty value")
    }

    test("transform between Iterables and Maps, using Total Transformer for inner type transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      Seq(1 -> 10, 2 -> 20).transformIntoPartial[Map[String, String]].asOption ==> Some(Map("1" -> "10", "2" -> "20"))
      ArrayBuffer(1 -> 10, 2 -> 20).transformIntoPartial[Map[Int, String]].asOption ==> Some(
        Map(1 -> "10", 2 -> "20")
      )
      Map(1 -> 10, 2 -> 20).transformIntoPartial[List[(String, String)]].asOption ==> Some(
        List("1" -> "10", "2" -> "20")
      )
      Map(1 -> 10, 2 -> 20).transformIntoPartial[Vector[(String, Int)]].asOption ==> Some(
        Vector("1" -> 10, "2" -> 20)
      )
    }

    test("transform between Iterables and Maps, using Partial Transformer for inner type transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      Seq("1" -> "10", "2" -> "20").transformIntoPartial[Map[Int, Int]].asOption ==> Some(Map(1 -> 10, 2 -> 20))
      ArrayBuffer("1" -> "10", "2" -> "20").transformIntoPartial[Map[String, Int]].asOption ==>
        Some(Map("1" -> 10, "2" -> 20))
      Map("1" -> "10", "2" -> "20").transformIntoPartial[List[(Int, Int)]].asOption ==> Some(List(1 -> 10, 2 -> 20))
      Map("1" -> "10", "2" -> "20").transformIntoPartial[Vector[(Int, String)]].asOption ==>
        Some(Vector(1 -> "10", 2 -> "20"))

      Seq("1" -> "10", "2" -> "x").transformIntoPartial[Map[Int, Int]].asOption ==> None
      ArrayBuffer("1" -> "x", "2" -> "y").transformIntoPartial[Map[String, Int]].asOption ==> None
      Map("x" -> "10", "y" -> "z").transformIntoPartial[List[(Int, Int)]].asOption ==> None
      Map("1" -> "10", "x" -> "20").transformIntoPartial[Vector[(Int, String)]].asOption ==> None

      Seq("1" -> "10", "2" -> "x").transformIntoPartial[Map[Int, Int]].asOption ==> None
      ArrayBuffer("1" -> "x", "2" -> "y").transformIntoPartial[Map[String, Int]].asOption ==> None
      Map("x" -> "10", "y" -> "z").transformIntoPartial[List[(Int, Int)]].asOption ==> None
      Map("1" -> "10", "x" -> "20").transformIntoPartial[Vector[(Int, String)]].asOption ==> None

      ArrayBuffer("1" -> "x", "2" -> "y")
        .transformIntoPartial[Map[String, Int]]
        .asErrorPathMessageStrings ==> Iterable("(0)._2" -> "empty value", "(1)._2" -> "empty value")
      Map("x" -> "10", "y" -> "z")
        .transformIntoPartial[List[(Int, Int)]]
        .asErrorPathMessageStrings ==> Iterable(
        "keys(x)" -> "empty value",
        "keys(y)" -> "empty value",
        "(y)" -> "empty value"
      )

      ArrayBuffer("1" -> "x", "2" -> "y")
        .transformIntoPartial[Map[String, Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(0)._2" -> "empty value")
      Map("x" -> "10", "y" -> "z")
        .transformIntoPartial[List[(Int, Int)]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("keys(x)" -> "empty value")
    }

    test("transform between Arrays and Maps, using Total Transformer for inner type transformation") {
      implicit val intPrinter: Transformer[Int, String] = _.toString

      Array(1 -> 10, 2 -> 20).transformIntoPartial[Map[String, String]].asOption ==> Some(
        Map("1" -> "10", "2" -> "20")
      )
      Array(1 -> 10, 2 -> 20).transformIntoPartial[Map[Int, String]].asOption ==> Some(Map(1 -> "10", 2 -> "20"))
      Map(1 -> 10, 2 -> 20).transformIntoPartial[Array[(String, String)]].asOption.get ==> Array(
        "1" -> "10",
        "2" -> "20"
      )
      Map(1 -> 10, 2 -> 20).transformIntoPartial[Array[(String, Int)]].asOption.get ==> Array("1" -> 10, "2" -> 20)
    }

    test("transform between Arrays and Maps, using Partial Transformer for inner type transformation") {
      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)

      Array("1" -> "10", "2" -> "20").transformIntoPartial[Map[Int, Int]].asOption ==> Some(Map(1 -> 10, 2 -> 20))
      Array("1" -> "10", "2" -> "20").transformIntoPartial[Map[String, Int]].asOption ==> Some(
        Map("1" -> 10, "2" -> 20)
      )
      Map("1" -> "10", "2" -> "20").transformIntoPartial[Array[(Int, Int)]].asOption.get ==> Array(1 -> 10, 2 -> 20)
      Map("1" -> "10", "2" -> "20").transformIntoPartial[Array[(Int, String)]].asOption.get ==> Array(
        1 -> "10",
        2 -> "20"
      )

      Array("x" -> "y", "z" -> "v").transformIntoPartial[Map[Int, Int]].asOption ==> None
      Array("1" -> "x", "2" -> "y").transformIntoPartial[Map[String, Int]].asOption ==> None
      Map("1" -> "10", "x" -> "20").transformIntoPartial[Array[(Int, Int)]].asOption ==> None
      Map("x" -> "10", "y" -> "20").transformIntoPartial[Array[(Int, String)]].asOption ==> None

      Array("1" -> "x", "2" -> "y")
        .transformIntoPartial[Map[String, Int]]
        .asErrorPathMessageStrings ==> Iterable("(0)._2" -> "empty value", "(1)._2" -> "empty value")
      Map("x" -> "10", "y" -> "20")
        .transformIntoPartial[Array[(Int, String)]]
        .asErrorPathMessageStrings ==> Iterable("keys(x)" -> "empty value", "keys(y)" -> "empty value")

      Array("1" -> "x", "2" -> "y")
        .transformIntoPartial[Map[String, Int]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("(0)._2" -> "empty value")
      Map("x" -> "10", "y" -> "20")
        .transformIntoPartial[Array[(Int, String)]](failFast = true)
        .asErrorPathMessageStrings ==> Iterable("keys(x)" -> "empty value")
    }

    test("flag .enableOptionDefaultsToNone") {

      case class Source(x: String)
      case class TargetWithOption(x: String, y: Option[Int])
      case class TargetWithOptionAndDefault(x: String, y: Option[Int] = Some(42))

      test("should be turned off by default and not allow compiling Option fields with missing source") {
        compileError("""Source("foo").intoPartial[TargetWithOption].transform.asOption""").check(
          "",
          "Chimney can't derive transformation from Source to TargetWithOption",
          "io.scalaland.chimney.PartialTransformerStdLibTypesSpec.TargetWithOption",
          "y: scala.Option - no accessor named y in source type io.scalaland.chimney.PartialTransformerStdLibTypesSpec.Source",
          "Consult https://scalalandio.github.io/chimney for usage examples."
        )
      }

      test("use None for fields without source nor default value when enabled") {
        Source("foo").intoPartial[TargetWithOption].enableOptionDefaultsToNone.transform.asOption ==> Some(
          TargetWithOption("foo", None)
        )
      }

      test("use None for fields without source but with default value when enabled but default values disabled") {
        Source("foo").intoPartial[TargetWithOptionAndDefault].enableOptionDefaultsToNone.transform.asOption ==> Some(
          TargetWithOptionAndDefault("foo", None)
        )
      }

      test("should be ignored when default value is set and default values enabled") {
        Source("foo")
          .intoPartial[TargetWithOption]
          .enableDefaultValues
          .enableOptionDefaultsToNone
          .transform
          .asOption ==> Some(
          TargetWithOption("foo", None)
        )
        Source("foo")
          .intoPartial[TargetWithOptionAndDefault]
          .enableDefaultValues
          .enableOptionDefaultsToNone
          .transform
          .asOption ==> Some(
          TargetWithOptionAndDefault(
            "foo",
            Some(42)
          )
        )
      }
    }

    test("flag .enableUnsafeOption") {

      case class Source(x: Option[Int])
      case class Target(x: String)

      test("should not supported for any case") {

        @unused implicit val intPrinter: Transformer[Int, String] = _.toString

        @unused implicit val intPartialParser: PartialTransformer[String, Int] =
          PartialTransformer(_.parseInt.toPartialResultOrString("bad int"))

        compileError("Option(10).intoPartial[String].enableUnsafeOption.transform").check(
          "",
          "Chimney can't derive transformation from Option[Int] to String",
          "java.lang.String",
          "derivation from option: scala.Option to java.lang.String is not supported in Chimney!",
          "Consult https://scalalandio.github.io/chimney for usage examples."
        )
        compileError("Option.empty[Int].intoPartial[String].enableUnsafeOption.transform").check(
          "",
          "Chimney can't derive transformation from Option[Int] to String",
          "java.lang.String",
          "derivation from option: scala.Option to java.lang.String is not supported in Chimney!",
          "Consult https://scalalandio.github.io/chimney for usage examples."
        )
        compileError("""Option("x").intoPartial[Int].enableUnsafeOption.transform""").check(
          "",
          "Chimney can't derive transformation from Option[String] to Int",
          "scala.Int",
          "derivation from option: scala.Option to scala.Int is not supported in Chimney!",
          "Consult https://scalalandio.github.io/chimney for usage examples."
        )
        compileError("""Option.empty[String].intoPartial[Int].enableUnsafeOption.transform""").check(
          "",
          "Chimney can't derive transformation from Option[String] to Int",
          "scala.Int",
          "derivation from option: scala.Option to scala.Int is not supported in Chimney!",
          "Consult https://scalalandio.github.io/chimney for usage examples."
        )
      }

      test("should be replaceable by explicitly provided Partial Transformer from Option") {
        implicit val optIntPrinter: PartialTransformer[Option[Int], String] =
          (i, _) => partial.Result.fromOption(i).map(_ * 2).map(_.toString)

        Option(10).transformIntoPartial[String].asOption ==> Some("20")
        Source(Some(10)).transformIntoPartial[Target].asOption ==> Some(Target("20"))
        Option.empty[Int].transformIntoPartial[String].asOption ==> None
        Source(None).transformIntoPartial[Target].asOption ==> None
      }
    }
  }
}
