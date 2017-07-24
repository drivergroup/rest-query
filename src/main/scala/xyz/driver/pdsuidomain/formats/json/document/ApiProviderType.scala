package xyz.driver.pdsuidomain.formats.json.document

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath}
import xyz.driver.pdsuicommon.domain.LongId
import xyz.driver.pdsuidomain.entities.ProviderType

final case class ApiProviderType(id: Long, name: String) {

  def toDomain = ProviderType(
    id = LongId(this.id),
    name = this.name
  )

}

object ApiProviderType {

  implicit val format: Format[ApiProviderType] = (
    (JsPath \ "id").format[Long] and
      (JsPath \ "name").format[String]
  )(ApiProviderType.apply, unlift(ApiProviderType.unapply))

  def fromDomain(providerType: ProviderType) = ApiProviderType(
    id = providerType.id.id,
    name = providerType.name
  )
}
