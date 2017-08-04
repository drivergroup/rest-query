package xyz.driver.pdsuidomain.formats.json.sprayformats

import spray.json._
import xyz.driver.pdsuicommon.domain.{LongId, StringId}
import xyz.driver.pdsuidomain.entities._
import xyz.driver.pdsuidomain.services.CriterionService.RichCriterion

object criterion {
  import DefaultJsonProtocol._
  import common._

  implicit val criterionLabelWriter = new JsonWriter[CriterionLabel] {
    override def write(obj: CriterionLabel) = JsObject(
      "labelId"    -> obj.labelId.toJson,
      "categoryId" -> obj.categoryId.toJson,
      "value" -> obj.value.map {
        case true  => "Yes"
        case false => "No"
      }.toJson,
      "isDefining" -> obj.isDefining.toJson
    )
  }

  def jsValueToCriterionLabel(json: JsValue, criterionId: LongId[Criterion]): CriterionLabel = json match {
    case JsObject(fields) =>
      val labelId = fields
        .get("labelId")
        .map(_.convertTo[LongId[Label]])

      val categoryId = fields
        .get("categoryId")
        .map(_.convertTo[LongId[Category]])

      val value = fields
        .get("value")
        .map(_.convertTo[String] match {
          case "Yes" => true
          case "No"  => false
          case other =>
            deserializationError(s"Unknown `value` of CriterionLabel object: expected `yes` or `no`, but got $other")
        })

      val isDefining = fields
        .get("isDefining")
        .map(_.convertTo[Boolean])
        .getOrElse(deserializationError(s"CriterionLabel json object does not contain `isDefining` field: $json"))

      CriterionLabel(
        id = LongId(0L),
        labelId = labelId,
        criterionId = criterionId,
        categoryId = categoryId,
        value = value,
        isDefining = isDefining
      )

    case _ => deserializationError(s"Expected Json Object as CriterionLabel, but got $json")
  }

  val richCriterionFormat = new RootJsonFormat[RichCriterion] {
    override def write(obj: RichCriterion): JsValue =
      JsObject(
        "id"         -> obj.criterion.id.toJson,
        "meta"       -> Option(obj.criterion.meta).toJson,
        "arms"       -> obj.armIds.toJson,
        "text"       -> obj.criterion.text.toJson,
        "isCompound" -> obj.criterion.isCompound.toJson,
        "labels"     -> obj.labels.map(_.toJson).toJson,
        "trialId"    -> obj.criterion.trialId.toJson
      )

    override def read(json: JsValue): RichCriterion = json match {
      case JsObject(fields) =>
        val id = fields
          .get("id")
          .map(_.convertTo[LongId[Criterion]])
          .getOrElse(LongId[Criterion](0))

        val trialId = fields
          .get("trialId")
          .map(_.convertTo[StringId[Trial]])
          .getOrElse(deserializationError(s"Criterion json object does not contain `trialId` field: $json"))

        val text = fields
          .get("text")
          .map(_.convertTo[String])

        val isCompound = fields
          .get("isCompound")
          .exists(_.convertTo[Boolean])

        val meta = fields
          .get("meta")
          .map(_.convertTo[String])
          .getOrElse("")

        val arms = fields
          .get("arms")
          .map(_.convertTo[List[LongId[Arm]]])
          .getOrElse(List.empty[LongId[Arm]])

        val labels = fields
          .get("labels")
          .map(_.convertTo[List[JsValue]])
          .map(_.map(l => jsValueToCriterionLabel(l, id)))
          .getOrElse(List.empty[CriterionLabel])

        RichCriterion(
          criterion = Criterion(
            id = id,
            trialId = trialId,
            text = text,
            isCompound = isCompound,
            meta = meta
          ),
          armIds = arms,
          labels = labels
        )

      case _ => deserializationError(s"Expected Json Object as Criterion, but got $json")
    }
  }

}
