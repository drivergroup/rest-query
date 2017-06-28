package xyz.driver.pdsuidomain.formats.json.document

import java.time.{LocalDate, ZoneId, ZonedDateTime}

import xyz.driver.pdsuidomain.entities._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import xyz.driver.pdsuicommon.json.JsonSerializer

final case class ApiDocument(id: Long,
                             recordId: Long,
                             physician: Option[String],
                             lastUpdate: Option[ZonedDateTime],
                             typeId: Option[Long],
                             startDate: Option[LocalDate],
                             endDate: Option[LocalDate],
                             provider: Option[String],
                             providerTypeId: Option[Long],
                             status: Option[String],
                             previousStatus: Option[String],
                             assignee: Option[Long],
                             previousAssignee: Option[Long],
                             meta: Option[String])

object ApiDocument {

  private val statusFormat = Format(
    Reads.StringReads.filter(ValidationError("unknown status")) {
      case x if DocumentUtils.statusFromString.isDefinedAt(x) => true
      case _ => false
    },
    Writes.StringWrites
  )

  implicit val format: Format[ApiDocument] = (
    (JsPath \ "id").format[Long] and
      (JsPath \ "recordId").format[Long] and
      (JsPath \ "physician").formatNullable[String] and
      (JsPath \ "lastUpdate").formatNullable[ZonedDateTime] and
      (JsPath \ "typeId").formatNullable[Long] and
      (JsPath \ "startDate").formatNullable[LocalDate] and
      (JsPath \ "endDate").formatNullable[LocalDate] and
      (JsPath \ "provider").formatNullable[String] and
      (JsPath \ "providerTypeId").formatNullable[Long] and
      (JsPath \ "status").formatNullable(statusFormat) and
      (JsPath \ "previousStatus").formatNullable(statusFormat) and
      (JsPath \ "assignee").formatNullable[Long] and
      (JsPath \ "previousAssignee").formatNullable[Long] and
      (JsPath \ "meta").formatNullable(Format(Reads { x => JsSuccess(Json.stringify(x)) }, Writes[String](Json.parse)))
    ) (ApiDocument.apply, unlift(ApiDocument.unapply))

  def fromDomain(document: Document): ApiDocument = {
    ApiDocument(
      id = document.id.id,
      recordId = document.recordId.id,
      physician = document.physician,
      lastUpdate = Option(document.lastUpdate).map(ZonedDateTime.of(_, ZoneId.of("Z"))),
      typeId = document.typeId.map(_.id),
      startDate = document.startDate,
      endDate = document.endDate,
      provider = document.providerName,
      providerTypeId = document.providerTypeId.map(_.id),
      status = Option(DocumentUtils.statusToString(document.status)),
      previousStatus = document.previousStatus.map(DocumentUtils.statusToString),
      assignee = document.assignee.map(_.id),
      previousAssignee = document.previousAssignee.map(_.id),
      meta = document.meta.map(meta => JsonSerializer.serialize(meta.content))
    )
  }
}