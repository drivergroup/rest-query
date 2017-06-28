package xyz.driver.pdsuidomain.formats.json.document

import java.time.{LocalDate, LocalDateTime}

import xyz.driver.pdsuicommon.domain.{LongId, TextJson}
import xyz.driver.pdsuidomain.entities.Document.Meta
import xyz.driver.pdsuidomain.entities._
import org.davidbild.tristate.Tristate
import org.davidbild.tristate.contrib.play.ToJsPathOpsFromJsPath
import play.api.data.validation._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import xyz.driver.pdsuicommon.json.{JsonSerializer, JsonValidationException}
import xyz.driver.pdsuicommon.validation.{AdditionalConstraints, JsonValidationErrors}

import scala.collection.breakOut
import scala.util.Try

final case class ApiPartialDocument(recordId: Option[Long],
                                    physician: Option[String],
                                    typeId: Tristate[Long],
                                    startDate: Tristate[LocalDate],
                                    endDate: Tristate[LocalDate],
                                    provider: Tristate[String],
                                    providerTypeId: Tristate[Long],
                                    status: Option[String],
                                    assignee: Tristate[Long],
                                    meta: Tristate[String]) {

  import xyz.driver.pdsuicommon.domain.User

  def applyTo(orig: Document): Document = Document(
    id = orig.id,
    status = status.map(DocumentUtils.statusFromString).getOrElse(orig.status),
    previousStatus = orig.previousStatus,
    assignee = assignee.map(LongId[User]).cata(Some(_), None, orig.assignee),
    previousAssignee = orig.previousAssignee,
    recordId = recordId.map(LongId[MedicalRecord]).getOrElse(orig.recordId),
    physician = physician.orElse(orig.physician),
    typeId = typeId.map(LongId[DocumentType]).cata(Some(_), None, orig.typeId),
    providerName = provider.cata(Some(_), None, orig.providerName),
    providerTypeId = providerTypeId.map(LongId[ProviderType]).cata(Some(_), None, orig.providerTypeId),
    meta = meta.cata(x => Some(TextJson(JsonSerializer.deserialize[Meta](x))), None, orig.meta),
    startDate = startDate.cata(Some(_), None, orig.startDate),
    endDate = endDate.cata(Some(_), None, orig.endDate),
    lastUpdate = LocalDateTime.MIN // Should update internally in a business logic module
  )

  def toDomain: Try[Document] = Try {
    val validation = Map(JsPath \ "recordId" -> AdditionalConstraints.optionNonEmptyConstraint(recordId))

    val validationErrors: JsonValidationErrors = validation.collect({
      case (fieldName, e: Invalid) => (fieldName, e.errors)
    })(breakOut)

    if (validationErrors.isEmpty) {
      Document(
        id = LongId(0),
        recordId = recordId.map(LongId[MedicalRecord]).get,
        status = Document.Status.New,
        physician = physician,
        typeId = typeId.map(LongId[DocumentType]).toOption,
        startDate = startDate.toOption,
        endDate = endDate.toOption,
        providerName = provider.toOption,
        providerTypeId = providerTypeId.map(LongId[ProviderType]).toOption,
        meta = meta.map(x => TextJson(JsonSerializer.deserialize[Meta](x))).toOption,
        previousStatus = None,
        assignee = None,
        previousAssignee = None,
        lastUpdate = LocalDateTime.MIN
      )
    } else {
      throw new JsonValidationException(validationErrors)
    }
  }
}

object ApiPartialDocument {

  private val reads: Reads[ApiPartialDocument] = (
    (JsPath \ "recordId").readNullable[Long] and
      (JsPath \ "physician").readNullable[String] and
      (JsPath \ "typeId").readTristate[Long] and
      (JsPath \ "startDate").readTristate[LocalDate] and
      (JsPath \ "endDate").readTristate[LocalDate] and
      (JsPath \ "provider").readTristate[String] and
      (JsPath \ "providerTypeId").readTristate[Long] and
      (JsPath \ "status").readNullable[String](Reads.of[String].filter(ValidationError("unknown status"))({
        case x if DocumentUtils.statusFromString.isDefinedAt(x) => true
        case _ => false
      })) and
      (JsPath \ "assignee").readTristate[Long] and
      (JsPath \ "meta").readTristate(Reads { x => JsSuccess(Json.stringify(x)) }).map {
        case Tristate.Present("{}") => Tristate.Absent
        case x => x
      }
    ) (ApiPartialDocument.apply _)

  private val writes: Writes[ApiPartialDocument] = (
    (JsPath \ "recordId").writeNullable[Long] and
      (JsPath \ "physician").writeNullable[String] and
      (JsPath \ "typeId").writeTristate[Long] and
      (JsPath \ "startDate").writeTristate[LocalDate] and
      (JsPath \ "endDate").writeTristate[LocalDate] and
      (JsPath \ "provider").writeTristate[String] and
      (JsPath \ "providerTypeId").writeTristate[Long] and
      (JsPath \ "status").writeNullable[String] and
      (JsPath \ "assignee").writeTristate[Long] and
      (JsPath \ "meta").writeTristate(Writes[String](Json.parse))
    ) (unlift(ApiPartialDocument.unapply))

  implicit val format: Format[ApiPartialDocument] = Format(reads, writes)
}