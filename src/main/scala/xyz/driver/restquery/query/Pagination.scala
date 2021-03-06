package xyz.driver.restquery.query

/**
  * @param pageNumber Starts with 1
  */
final case class Pagination(pageSize: Int, pageNumber: Int) {
  def offset: Int = pageSize * (pageNumber - 1)
}

object Pagination {

  // @see https://driverinc.atlassian.net/wiki/display/RA/REST+API+Specification#RESTAPISpecification-CommonRequestQueryParametersForWebServices
  val Default = Pagination(pageSize = 100, pageNumber = 1)
}
