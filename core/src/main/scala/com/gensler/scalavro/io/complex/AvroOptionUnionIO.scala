package com.gensler.scalavro.io.complex

import com.gensler.scalavro.io.AvroTypeIO
import com.gensler.scalavro.io.primitive.{ AvroLongIO, AvroNullIO }
import com.gensler.scalavro.types.AvroType
import com.gensler.scalavro.types.complex.AvroUnion
import com.gensler.scalavro.error.{ AvroSerializationException, AvroDeserializationException }
import com.gensler.scalavro.util.Union
import com.gensler.scalavro.util.Union._

import org.apache.avro.io.{ BinaryEncoder, BinaryDecoder }

import scala.collection.mutable
import scala.util.{ Try, Success, Failure }
import scala.reflect.runtime.universe._

import java.io.{ InputStream, OutputStream }

private[scalavro] case class AvroOptionUnionIO[U <: Union.not[_]: TypeTag, T <: Option[_]: TypeTag](
    avroType: AvroUnion[U, T]) extends AvroUnionIO[U, T] {

  // IMPORTANT:
  // null is the 0th index in the union, per AvroType.fromType
  val NULL_INDEX = 0L
  val NON_NULL_INDEX = 1L

  val TypeRef(_, _, List(innerType)) = typeOf[T]

  val innerAvroType = avroType.memberAvroTypes.find { at => innerType <:< at.tag.tpe }.get

  protected[scalavro] def write[X <: T: TypeTag](
    obj: X,
    encoder: BinaryEncoder,
    references: mutable.Map[Any, Long],
    topLevel: Boolean): Unit = {

    AvroLongIO.write(if (obj.isDefined) NON_NULL_INDEX else NULL_INDEX, encoder)
    writeHelper(obj, encoder, references, topLevel)(typeTag[X], innerAvroType.tag)
    encoder.flush
  }

  protected[this] def writeHelper[X <: T: TypeTag, A: TypeTag](
    obj: X,
    encoder: BinaryEncoder,
    references: mutable.Map[Any, Long],
    topLevel: Boolean) = obj match {

    case Some(value) => innerAvroType.asInstanceOf[AvroType[A]].io.write(
      value.asInstanceOf[A],
      encoder,
      references,
      false
    )
    case None => AvroNullIO.write((), encoder, references, false)
  }

  protected[scalavro] def read(
    decoder: BinaryDecoder,
    references: mutable.ArrayBuffer[Any],
    topLevel: Boolean) = {

    readHelper(decoder, references)(innerAvroType.tag).asInstanceOf[T]
  }

  def readHelper[A: TypeTag](
    decoder: BinaryDecoder,
    references: mutable.ArrayBuffer[Any]) = {

    (AvroLongIO read decoder) match {
      case NON_NULL_INDEX => Some(
        innerAvroType.io.read(decoder, references, false).asInstanceOf[A]
      )
      case NULL_INDEX => None
      case index: Long => throw new AvroDeserializationException[T](
        detailedMessage = "Encountered an index that was not zero or one: [%s]".format(index)
      )
    }
  }
}