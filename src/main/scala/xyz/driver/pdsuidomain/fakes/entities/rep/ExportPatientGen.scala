package xyz.driver.pdsuidomain.fakes.entities.rep

import xyz.driver.core.generators._
import xyz.driver.pdsuicommon.domain.{LongId, UuidId}
import xyz.driver.pdsuidomain.entities.{Document, ExtractedData, Label, RecordRequestId}
import xyz.driver.pdsuidomain.entities.export.patient.{
  ExportPatientLabel,
  ExportPatientLabelEvidence,
  ExportPatientLabelEvidenceDocument,
  ExportPatientWithLabels
}
import xyz.driver.pdsuidomain.fakes.entities.common.{nextLocalDate, nextLongId}

object ExportPatientGen {
  private val maxItemsInCollectionNumber = 3

  def nextExportPatientLabelEvidenceDocument(documentId: LongId[Document]): ExportPatientLabelEvidenceDocument = {
    ExportPatientLabelEvidenceDocument(
      documentId = documentId,
      requestId = RecordRequestId(nextUuid()),
      documentType = nextString(),
      providerType = nextString(),
      date = nextLocalDate
    )
  }

  def nextExportPatientLabelEvidence(documentId: LongId[Document]): ExportPatientLabelEvidence = {
    ExportPatientLabelEvidence(
      id = nextLongId[ExtractedData],
      value = Common.nextFuzzyValue(),
      evidenceText = nextString(),
      document = nextExportPatientLabelEvidenceDocument(documentId)
    )
  }

  def nextExportPatientLabel(documentId: LongId[Document]): ExportPatientLabel = {
    ExportPatientLabel(
      id = nextLongId[Label],
      evidences = List.fill(
        nextInt(maxItemsInCollectionNumber, minValue = 0)
      )(nextExportPatientLabelEvidence(documentId))
    )
  }

  def nextExportPatientWithLabels(documentId: LongId[Document]): ExportPatientWithLabels = {
    ExportPatientWithLabels(
      patientId = UuidId[xyz.driver.pdsuidomain.entities.Patient](nextUuid()),
      labelVersion = scala.util.Random.nextLong(),
      labels = List.fill(
        nextInt(maxItemsInCollectionNumber, minValue = 0)
      )(nextExportPatientLabel(documentId))
    )
  }

}
