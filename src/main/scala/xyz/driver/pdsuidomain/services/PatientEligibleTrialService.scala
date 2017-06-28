package xyz.driver.pdsuidomain.services

import xyz.driver.pdsuicommon.auth.AuthenticatedRequestContext
import xyz.driver.pdsuicommon.db.{Pagination, SearchFilterExpr, Sorting}
import xyz.driver.pdsuicommon.domain.{LongId, UuidId}
import xyz.driver.pdsuicommon.error.DomainError
import xyz.driver.pdsuicommon.logging._
import xyz.driver.pdsuidomain.entities.{Arm, Trial, _}

import scala.concurrent.Future

object PatientEligibleTrialService {

  trait DefaultNotFoundError {
    def userMessage: String = "Patient eligible trial not found"
  }

  trait DefaultPatientNotFoundError {
    def userMessage: String = "Patient not found"
  }

  trait DefaultAccessDeniedError {
    def userMessage: String = "Access denied"
  }

  case class RichPatientEligibleTrial(trial: Trial,
                                      group: PatientTrialArmGroupView,
                                      arms: List[Arm])
  object RichPatientEligibleTrial {
    implicit def toPhiString(x: RichPatientEligibleTrial): PhiString = {
      phi"RichPatientEligibleTrial(group=${x.group}, trial=${x.trial}, arms=${x.arms})"
    }
  }

  sealed trait GetListReply
  object GetListReply {
    case class EntityList(xs: Seq[RichPatientEligibleTrial], totalFound: Int)
      extends GetListReply

    case object AuthorizationError
      extends GetListReply with DomainError.AuthorizationError with DefaultAccessDeniedError

    case object PatientNotFoundError
      extends GetListReply with DefaultPatientNotFoundError with DomainError.NotFoundError

    case class CommonError(userMessage: String) extends GetListReply with DomainError
  }

  sealed trait GetByIdReply
  object GetByIdReply {
    case class Entity(x: RichPatientEligibleTrial) extends GetByIdReply

    type Error = GetByIdReply with DomainError

    case object NotFoundError
      extends GetByIdReply with DefaultNotFoundError with DomainError.NotFoundError

    case object PatientNotFoundError
      extends GetByIdReply with DefaultPatientNotFoundError with DomainError.NotFoundError

    case object AuthorizationError
      extends GetByIdReply with DomainError.AuthorizationError with DefaultAccessDeniedError

    case class CommonError(userMessage: String) extends GetByIdReply with DomainError

    implicit def toPhiString(reply: GetByIdReply): PhiString = reply match {
      case x: DomainError => phi"GetByIdReply.Error($x)"
      case Entity(x) => phi"GetByIdReply.Entity($x)"
    }
  }

  sealed trait GetCriterionListOfGroupReply
  object GetCriterionListOfGroupReply {
    case class EntityList(xs: Seq[(PatientCriterion, LongId[Label], List[Arm], Boolean)], totalFound: Int)
      extends GetCriterionListOfGroupReply

    type Error = GetCriterionListOfGroupReply with DomainError

    case object AuthorizationError
      extends GetCriterionListOfGroupReply with DomainError.AuthorizationError with DefaultAccessDeniedError

    case object NotFoundError
      extends GetCriterionListOfGroupReply with DefaultNotFoundError with DomainError.NotFoundError

    case object PatientNotFoundError
      extends GetCriterionListOfGroupReply with DefaultPatientNotFoundError with DomainError.NotFoundError

    case class CommonError(userMessage: String) extends GetCriterionListOfGroupReply with DomainError
  }

  sealed trait UpdateReply
  object UpdateReply {
    type Error = UpdateReply with DomainError

    case class Updated(updated: RichPatientEligibleTrial) extends UpdateReply

    case object NotFoundError
      extends UpdateReply with DefaultNotFoundError with DomainError.NotFoundError

    case object PatientNotFoundError
      extends UpdateReply with DefaultPatientNotFoundError with DomainError.NotFoundError

    case object AuthorizationError
      extends UpdateReply with DomainError.AuthorizationError with DefaultAccessDeniedError

    case class CommonError(userMessage: String)
      extends UpdateReply with DomainError

    implicit def toPhiString(reply: UpdateReply): PhiString = reply match {
      case Updated(x) => phi"Updated($x)"
      case x: Error => DomainError.toPhiString(x)
    }
  }
}

trait PatientEligibleTrialService {

  import PatientEligibleTrialService._

  def getAll(patientId: UuidId[Patient],
             filter: SearchFilterExpr = SearchFilterExpr.Empty,
             sorting: Option[Sorting] = None,
             pagination: Option[Pagination] = None)
            (implicit requestContext: AuthenticatedRequestContext): Future[GetListReply]

  def getById(patientId: UuidId[Patient],
              id: LongId[PatientTrialArmGroup])
             (implicit requestContext: AuthenticatedRequestContext): Future[GetByIdReply]

  def getCriterionListByGroupId(patientId: UuidId[Patient],
                                id: LongId[PatientTrialArmGroup])
                               (implicit requestContext: AuthenticatedRequestContext): Future[GetCriterionListOfGroupReply]

  def update(origEligibleTrialWithTrial: RichPatientEligibleTrial,
             draftPatientTrialArmGroup: PatientTrialArmGroupView)
            (implicit requestContext: AuthenticatedRequestContext): Future[UpdateReply]
}