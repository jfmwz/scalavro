package com.gensler.scalavro.types.complex

import com.gensler.scalavro.types.AvroType
import com.gensler.scalavro.types.primitive.AvroNull
import scala.reflect.runtime.universe._
import scala.util.Try
import spray.json._

class AvroMap[T: TypeTag] extends AvroType[Map[String, T]] {

  type ItemType = T

  val typeName = "map"

  def write(obj: Map[String, T]): Seq[Byte] = ???

  def read(bytes: Seq[Byte]) = Try {
    ???.asInstanceOf[Map[String, T]]
  }

  override def schema() = Map(
    "type"  -> typeName,
    "values" -> AvroType.fromType[T].toOption.getOrElse(AvroNull).typeName
  ).toJson

}
