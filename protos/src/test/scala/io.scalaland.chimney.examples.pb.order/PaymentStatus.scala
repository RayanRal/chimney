// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package io.scalaland.chimney.examples.pb.order

sealed trait PaymentStatus

@SerialVersionUID(0L)
final case class PaymentRequested(
) extends io.scalaland.chimney.examples.pb.order.PaymentStatus

@SerialVersionUID(0L)
final case class PaymentCreated(
    externalId: _root_.scala.Predef.String = ""
) extends io.scalaland.chimney.examples.pb.order.PaymentStatus

@SerialVersionUID(0L)
final case class PaymentSucceeded(
) extends io.scalaland.chimney.examples.pb.order.PaymentStatus

@SerialVersionUID(0L)
final case class PaymentFailed(
) extends io.scalaland.chimney.examples.pb.order.PaymentStatus
