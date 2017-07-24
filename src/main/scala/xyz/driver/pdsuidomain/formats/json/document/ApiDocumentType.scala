package xyz.driver.pdsuidomain.formats.json.document

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath}
import xyz.driver.pdsuicommon.domain.LongId
import xyz.driver.pdsuidomain.entities.DocumentType

final case class ApiDocumentType(id: Long, name: String) {

  def toDomain = DocumentType(
    id = LongId(this.id),
    name = this.name
  )

}

object ApiDocumentType {

  implicit val format: Format[ApiDocumentType] = (
    (JsPath \ "id").format[Long] and
      (JsPath \ "name").format[String]
  )(ApiDocumentType.apply, unlift(ApiDocumentType.unapply))

  def fromDomain(documentType: DocumentType) = ApiDocumentType(
    id = documentType.id.id,
    name = documentType.name
  )
}
