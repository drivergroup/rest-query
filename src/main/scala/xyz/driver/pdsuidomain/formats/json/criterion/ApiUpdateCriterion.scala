package xyz.driver.pdsuidomain.formats.json.criterion

import xyz.driver.pdsuicommon.domain.LongId
import xyz.driver.pdsuicommon.json.Serialization.seqJsonFormat
import xyz.driver.pdsuidomain.entities.{EligibilityArm, Criterion}
import org.davidbild.tristate._
import org.davidbild.tristate.contrib.play.ToJsPathOpsFromJsPath
import play.api.libs.functional.syntax._
import play.api.libs.json._
import xyz.driver.pdsuidomain.formats.json.label.ApiCriterionLabel
import xyz.driver.pdsuidomain.services.CriterionService.RichCriterion

final case class ApiUpdateCriterion(meta: Tristate[String],
                                    arms: Tristate[Seq[Long]],
                                    text: Option[String],
                                    isCompound: Option[Boolean],
                                    inclusion: Tristate[Boolean],
                                    labels: Tristate[Seq[ApiCriterionLabel]]) {

  def applyTo(orig: RichCriterion): RichCriterion = RichCriterion(
    criterion = applyTo(orig.criterion),
    armIds = arms.cata(_.map(LongId[EligibilityArm]), Seq.empty, orig.armIds),
    labels = labels.cata(_.map(_.toDomain(orig.criterion.id)), Seq.empty, orig.labels)
  )

  private def applyTo(orig: Criterion): Criterion = Criterion(
    id = orig.id,
    meta = meta.cata(identity, "{}", orig.meta),
    text = text.orElse(orig.text),
    isCompound = isCompound.getOrElse(orig.isCompound),
    trialId = orig.trialId,
    inclusion = inclusion.cata(x => Some(x), None, orig.inclusion)
  )
}

object ApiUpdateCriterion {

  private val reads: Reads[ApiUpdateCriterion] = (
    (JsPath \ "meta")
      .readTristate(Reads { x =>
        JsSuccess(Json.stringify(x))
      })
      .map {
        case Tristate.Present("{}") => Tristate.Absent
        case x                      => x
      } and
      (JsPath \ "arms").readTristate(seqJsonFormat[Long]) and
      (JsPath \ "text").readNullable[String] and
      (JsPath \ "isCompound").readNullable[Boolean] and
      (JsPath \ "inclusion").readTristate[Boolean] and
      (JsPath \ "labels").readTristate(seqJsonFormat[ApiCriterionLabel])
  )(ApiUpdateCriterion.apply _)

  private val writes: Writes[ApiUpdateCriterion] = (
    (JsPath \ "meta").writeTristate(Writes[String](Json.parse)) and
      (JsPath \ "arms").writeTristate(seqJsonFormat[Long]) and
      (JsPath \ "text").writeNullable[String] and
      (JsPath \ "isCompound").writeNullable[Boolean] and
      (JsPath \ "inclusion").writeTristate[Boolean] and
      (JsPath \ "labels").writeTristate(seqJsonFormat[ApiCriterionLabel])
  )(unlift(ApiUpdateCriterion.unapply))

  implicit val format: Format[ApiUpdateCriterion] = Format(reads, writes)
}
