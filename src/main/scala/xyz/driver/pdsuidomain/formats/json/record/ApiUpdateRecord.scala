package xyz.driver.pdsuidomain.formats.json.record

import xyz.driver.pdsuidomain.entities.MedicalRecord.Meta
import xyz.driver.pdsuidomain.entities._
import xyz.driver.pdsuicommon.domain.{LongId, TextJson, User}
import org.davidbild.tristate.Tristate
import org.davidbild.tristate.contrib.play.ToJsPathOpsFromJsPath
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import xyz.driver.pdsuicommon.json.JsonSerializer

final case class ApiUpdateRecord(status: Option[String],
                                 assignee: Tristate[Long],
                                 meta: Tristate[String]) {

  def applyTo(orig: MedicalRecord): MedicalRecord = {
    orig.copy(
      status = status.map(MedicalRecordStatus.statusFromString).getOrElse(orig.status),
      assignee = assignee.map(LongId[User]).cata(Some(_), None, orig.assignee),
      meta = meta.cata(x => Some(TextJson(JsonSerializer.deserialize[List[Meta]](x))), None, orig.meta)
    )
  }
}

object ApiUpdateRecord {

  private val reads: Reads[ApiUpdateRecord] = (
    (JsPath \ "status").readNullable[String](Reads.of[String].filter(ValidationError("unknown status"))({
      case x if MedicalRecordStatus.statusFromString.isDefinedAt(x) => true
      case _ => false
    })) and
      (JsPath \ "assignee").readTristate[Long] and
      (JsPath \ "meta").readTristate(Reads { x => JsSuccess(Json.stringify(x)) }).map {
        case Tristate.Present("{}") => Tristate.Absent
        case x => x
      }
    ) (ApiUpdateRecord.apply _)

  private val writes: Writes[ApiUpdateRecord] = (
    (JsPath \ "status").writeNullable[String] and
      (JsPath \ "assignee").writeTristate[Long] and
      (JsPath \ "meta").writeTristate(Writes[String](Json.parse))
    ) (unlift(ApiUpdateRecord.unapply))

  implicit val format: Format[ApiUpdateRecord] = Format(reads, writes)
}