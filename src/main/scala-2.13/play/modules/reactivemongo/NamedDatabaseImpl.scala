package play.modules.reactivemongo;

import java.io.Serializable

final class NamedDatabaseImpl(val value: String)
    extends NamedDatabase
    with Serializable {

  assert(value != null)

  override def hashCode: Int = (127 * "value".hashCode()) ^ value.hashCode()

  override def equals(that: Any): Boolean = that match {
    case other: NamedDatabase =>
      other.value == this.value

    case _ =>
      false
  }

  override def toString: String =
    s"@${classOf[NamedDatabase].getName}(value=${value})"

  def annotationType = classOf[NamedDatabase]
}
