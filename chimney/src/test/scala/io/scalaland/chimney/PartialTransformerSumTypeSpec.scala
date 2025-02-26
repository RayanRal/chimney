package io.scalaland.chimney

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.examples.*
import io.scalaland.chimney.utils.OptionUtils.*
import utest.*

object PartialTransformerSumTypeSpec extends TestSuite {

  val tests = Tests {

    test(
      """transform sealed hierarchies from "subset" of case objects to "superset" of case objects without modifiers"""
    ) {
      (colors1.Red: colors1.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Red)
      (colors1.Green: colors1.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Green)
      (colors1.Blue: colors1.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Blue)
    }

    test(
      """transform nested sealed hierarchies between flat and nested hierarchies of case objects without modifiers"""
    ) {
      (colors2.Red: colors2.Color).transformIntoPartial[colors3.Color].asOption ==> Some(colors3.Red)
      (colors2.Green: colors2.Color).transformIntoPartial[colors3.Color].asOption ==> Some(colors3.Green)
      (colors2.Blue: colors2.Color).transformIntoPartial[colors3.Color].asOption ==> Some(colors3.Blue)
      (colors2.Black: colors2.Color).transformIntoPartial[colors3.Color].asOption ==> Some(colors3.Black)

      (colors3.Red: colors3.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Red)
      (colors3.Green: colors3.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Green)
      (colors3.Blue: colors3.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Blue)
      (colors3.Black: colors3.Color).transformIntoPartial[colors2.Color].asOption ==> Some(colors2.Black)
    }

    test(
      """transforming flat hierarchies from "subset" of case classes to "superset" of case classes without modifiers when common corresponding types are transformable with Total Transformer"""
    ) {
      implicit val intToDoubleTransformer: Transformer[Int, Double] = (_: Int).toDouble

      (shapes1.Triangle(shapes1.Point(0, 0), shapes1.Point(2, 2), shapes1.Point(2, 0)): shapes1.Shape)
        .transformIntoPartial[shapes3.Shape]
        .asOption ==>
        Some(shapes3.Triangle(shapes3.Point(2.0, 0.0), shapes3.Point(2.0, 2.0), shapes3.Point(0.0, 0.0)))
      (shapes1.Rectangle(shapes1.Point(0, 0), shapes1.Point(6, 4)): shapes1.Shape)
        .transformIntoPartial[shapes3.Shape]
        .asOption ==>
        Some(shapes3.Rectangle(shapes3.Point(0.0, 0.0), shapes3.Point(6.0, 4.0)))

      implicit val intToStringTransformer: Transformer[Int, String] = (_: Int).toString
      import numbers.*, ScalesPartialTransformer.shortToLongTotalInner

      (short.Zero: short.NumScale[Int, Nothing])
        .transformIntoPartial[long.NumScale[String]]
        .asOption ==> Some(long.Zero)
      (short.Million(4): short.NumScale[Int, Nothing])
        .transformIntoPartial[long.NumScale[String]]
        .asOption ==> Some(long.Million("4"))
      (short.Billion(2): short.NumScale[Int, Nothing])
        .transformIntoPartial[long.NumScale[String]]
        .asOption ==> Some(long.Milliard("2"))
      (short.Trillion(100): short.NumScale[Int, Nothing])
        .transformIntoPartial[long.NumScale[String]]
        .asOption ==> Some(long.Billion("100"))
    }

    test(
      """transforming flat hierarchies from "subset" of case classes to "superset" of case classes without modifiers when common corresponding types are transformable with Partial Transformer"""
    ) {
      implicit val intToDoubleTransformer: PartialTransformer[Int, Double] =
        (a: Int, _) => partial.Result.fromValue(a.toDouble)

      (shapes1.Triangle(shapes1.Point(0, 0), shapes1.Point(2, 2), shapes1.Point(2, 0)): shapes1.Shape)
        .transformIntoPartial[shapes3.Shape]
        .asOption ==>
        Some(shapes3.Triangle(shapes3.Point(2.0, 0.0), shapes3.Point(2.0, 2.0), shapes3.Point(0.0, 0.0)))
      (shapes1.Rectangle(shapes1.Point(0, 0), shapes1.Point(6, 4)): shapes1.Shape)
        .transformIntoPartial[shapes3.Shape]
        .asOption ==>
        Some(shapes3.Rectangle(shapes3.Point(0.0, 0.0), shapes3.Point(6.0, 4.0)))

      implicit val intParserOpt: PartialTransformer[String, Int] =
        PartialTransformer(_.parseInt.toPartialResult)
      import numbers.*, ScalesPartialTransformer.shortToLongPartialInner

      (short.Zero: short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> Some(long.Zero)
      (short.Million("4"): short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> Some(long.Million(4))
      (short.Billion("2"): short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> Some(long.Milliard(2))
      (short.Trillion("100"): short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> Some(long.Billion(100))

      (short.Million("x"): short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> None
      (short.Billion("x"): short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> None
      (short.Trillion("x"): short.NumScale[String, Nothing])
        .transformIntoPartial[long.NumScale[Int]]
        .asOption ==> None
    }

    test(
      """transforming nested sealed hierarchies from "subset" of case classes to "superset" of case classes without modifiers when common corresponding types are transformable"""
    ) {
      (shapes3.Triangle(shapes3.Point(2.0, 0.0), shapes3.Point(2.0, 2.0), shapes3.Point(0.0, 0.0)): shapes3.Shape)
        .transformIntoPartial[shapes4.Shape]
        .asOption ==>
        Some(shapes4.Triangle(shapes4.Point(2.0, 0.0), shapes4.Point(2.0, 2.0), shapes4.Point(0.0, 0.0)))
      (shapes3.Rectangle(shapes3.Point(2.0, 0.0), shapes3.Point(2.0, 2.0)): shapes3.Shape)
        .transformIntoPartial[shapes4.Shape]
        .asOption ==>
        Some(shapes4.Rectangle(shapes4.Point(2.0, 0.0), shapes4.Point(2.0, 2.0)))
      (shapes4.Triangle(shapes4.Point(2.0, 0.0), shapes4.Point(2.0, 2.0), shapes4.Point(0.0, 0.0)): shapes4.Shape)
        .transformIntoPartial[shapes3.Shape]
        .asOption ==>
        Some(shapes3.Triangle(shapes3.Point(2.0, 0.0), shapes3.Point(2.0, 2.0), shapes3.Point(0.0, 0.0)))
      (shapes4.Rectangle(shapes4.Point(2.0, 0.0), shapes4.Point(2.0, 2.0)): shapes4.Shape)
        .transformIntoPartial[shapes3.Shape]
        .asOption ==>
        Some(shapes3.Rectangle(shapes3.Point(2.0, 0.0), shapes3.Point(2.0, 2.0)))
    }

    test("setting .withCoproductInstance(mapping)") {

      test(
        """should be absent by default and not allow transforming "superset" of case class to "subset" of case objects"""
      ) {
        compileError("""(colors2.Black: colors2.Color).transformIntoPartial[colors1.Color]""").check(
          "",
          "Chimney can't derive transformation from io.scalaland.chimney.examples.colors2.Color to io.scalaland.chimney.examples.colors1.Color",
          "io.scalaland.chimney.examples.colors1.Color",
          "can't transform coproduct instance io.scalaland.chimney.examples.colors2.Black to io.scalaland.chimney.examples.colors1.Color",
          "Consult https://scalalandio.github.io/chimney for usage examples."
        )
      }

      test(
        """transform sealed hierarchies from "superset" of case objects to "subset" of case objects when user-provided mapping handled additional cases"""
      ) {
        def blackIsRed(b: colors2.Black.type): colors1.Color =
          colors1.Red

        (colors2.Black: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstance(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Red)

        (colors2.Red: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstance(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Red)

        (colors2.Green: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstance(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Green)

        (colors2.Blue: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstance(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Blue)
      }

      test(
        """transform sealed hierarchies from "superset" of case classes to "subset" of case classes when user-provided mapping handled non-trivial cases"""
      ) {
        def triangleToPolygon(t: shapes1.Triangle): shapes2.Shape =
          shapes2.Polygon(
            List(
              t.p1.transformInto[shapes2.Point],
              t.p2.transformInto[shapes2.Point],
              t.p3.transformInto[shapes2.Point]
            )
          )

        def rectangleToPolygon(r: shapes1.Rectangle): shapes2.Shape =
          shapes2.Polygon(
            List(
              r.p1.transformInto[shapes2.Point],
              shapes2.Point(r.p1.x, r.p2.y),
              r.p2.transformInto[shapes2.Point],
              shapes2.Point(r.p2.x, r.p1.y)
            )
          )

        val triangle: shapes1.Shape =
          shapes1.Triangle(shapes1.Point(0, 0), shapes1.Point(2, 2), shapes1.Point(2, 0))

        triangle
          .intoPartial[shapes2.Shape]
          .withCoproductInstance(triangleToPolygon)
          .withCoproductInstance(rectangleToPolygon)
          .transform
          .asOption ==> Some(shapes2.Polygon(List(shapes2.Point(0, 0), shapes2.Point(2, 2), shapes2.Point(2, 0))))

        val rectangle: shapes1.Shape =
          shapes1.Rectangle(shapes1.Point(0, 0), shapes1.Point(6, 4))

        rectangle
          .intoPartial[shapes2.Shape]
          .withCoproductInstance[shapes1.Shape] {
            case r: shapes1.Rectangle => rectangleToPolygon(r)
            case t: shapes1.Triangle  => triangleToPolygon(t)
          }
          .transform
          .asOption ==> Some(
          shapes2.Polygon(
            List(shapes2.Point(0, 0), shapes2.Point(0, 4), shapes2.Point(6, 4), shapes2.Point(6, 0))
          )
        )
      }
    }

    test("setting .withCoproductInstancePartial[Subtype](mapping)") {

      test(
        """transform sealed hierarchies from "superset" of case objects to "subset" of case objects when user-provided mapping handled additional cases"""
      ) {
        def blackIsRed(b: colors2.Black.type): partial.Result[colors1.Color] =
          partial.Result.fromEmpty

        (colors2.Black: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstancePartial(blackIsRed)
          .transform
          .asOption ==> None

        (colors2.Red: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstancePartial(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Red)

        (colors2.Green: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstancePartial(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Green)

        (colors2.Blue: colors2.Color)
          .intoPartial[colors1.Color]
          .withCoproductInstancePartial(blackIsRed)
          .transform
          .asOption ==> Some(colors1.Blue)
      }

      test(
        """transform sealed hierarchies from "superset" of case classes to "subset" of case classes when user-provided mapping handled non-trivial cases"""
      ) {
        def triangleToPolygon(t: shapes1.Triangle): partial.Result[shapes2.Shape] =
          partial.Result.fromValue(
            shapes2.Polygon(
              List(
                t.p1.transformInto[shapes2.Point],
                t.p2.transformInto[shapes2.Point],
                t.p3.transformInto[shapes2.Point]
              )
            )
          )

        def rectangleToPolygon(r: shapes1.Rectangle): partial.Result[shapes2.Shape] =
          partial.Result.fromValue(
            shapes2.Polygon(
              List(
                r.p1.transformInto[shapes2.Point],
                shapes2.Point(r.p1.x, r.p2.y),
                r.p2.transformInto[shapes2.Point],
                shapes2.Point(r.p2.x, r.p1.y)
              )
            )
          )

        val triangle: shapes1.Shape =
          shapes1.Triangle(shapes1.Point(0, 0), shapes1.Point(2, 2), shapes1.Point(2, 0))

        triangle
          .intoPartial[shapes2.Shape]
          .withCoproductInstancePartial(triangleToPolygon)
          .withCoproductInstancePartial(rectangleToPolygon)
          .transform
          .asOption ==> Some(shapes2.Polygon(List(shapes2.Point(0, 0), shapes2.Point(2, 2), shapes2.Point(2, 0))))

        val rectangle: shapes1.Shape =
          shapes1.Rectangle(shapes1.Point(0, 0), shapes1.Point(6, 4))

        rectangle
          .intoPartial[shapes2.Shape]
          .withCoproductInstancePartial[shapes1.Shape] {
            case r: shapes1.Rectangle => rectangleToPolygon(r)
            case t: shapes1.Triangle  => triangleToPolygon(t)
          }
          .transform
          .asOption ==> Some(
          shapes2.Polygon(
            List(shapes2.Point(0, 0), shapes2.Point(0, 4), shapes2.Point(6, 4), shapes2.Point(6, 0))
          )
        )
      }
    }
  }
}
