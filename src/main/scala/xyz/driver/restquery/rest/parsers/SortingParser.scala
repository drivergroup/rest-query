package xyz.driver.restquery.rest.parsers

import fastparse.all._
import fastparse.core.Parsed
import xyz.driver.restquery.query.{Sorting, SortingOrder}
import xyz.driver.restquery.utils.Utils._

import scala.util.Try

object SortingParser {

  private val sortingOrderParser: Parser[SortingOrder] = P("-".!.?).map {
    case Some(_) => SortingOrder.Descending
    case None    => SortingOrder.Ascending
  }

  private def dimensionSortingParser(validDimensions: Seq[String]): Parser[Sorting.Dimension] = {
    P(sortingOrderParser ~ StringIn(validDimensions: _*).!).map {
      case (sortingOrder, field) =>
        val prefixedFields = field.split("\\.", 2)
        prefixedFields.size match {
          case 1 => Sorting.Dimension(None, toSnakeCase(field), sortingOrder)
          case 2 =>
            Sorting.Dimension(
              Some(prefixedFields.head).map(toSnakeCase),
              toSnakeCase(prefixedFields.last),
              sortingOrder)
        }
    }
  }

  private def sequentialSortingParser(validDimensions: Seq[String]): Parser[Sorting.Sequential] = {
    P(dimensionSortingParser(validDimensions).rep(min = 1, sep = ",") ~ End).map { dimensions =>
      Sorting.Sequential(dimensions)
    }
  }

  @deprecated("play-akka transition", "0")
  def parse(validDimensions: Set[String], query: Map[String, Seq[String]]): Try[Sorting] =
    parse(validDimensions, query.toSeq.flatMap {
      case (key, values) =>
        values.map(value => key -> value)
    })

  def parse(validDimensions: Set[String], query: Seq[(String, String)]): Try[Sorting] = Try {
    query.toList.collect { case ("sort", value) => value } match {
      case Nil => Sorting.Sequential(Seq.empty)

      case rawSorting :: Nil =>
        val parser = sequentialSortingParser(validDimensions.toSeq)
        parser.parse(rawSorting) match {
          case Parsed.Success(x, _) => x
          case e: Parsed.Failure[_, _] =>
            throw new ParseQueryArgException("sort" -> formatFailure(e))
        }

      case _ => throw new ParseQueryArgException("sort" -> "multiple sections are not allowed")
    }
  }

  private def formatFailure(e: Parsed.Failure[_, _]): String = {
    fastparse.core.ParseError.msg(e.extra.input, e.extra.traced.expected, e.index)
  }

}
