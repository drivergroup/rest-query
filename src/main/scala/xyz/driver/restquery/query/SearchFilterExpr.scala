package xyz.driver.restquery.query

import xyz.driver.restquery.query.SearchFilterBinaryOperation.Eq
import xyz.driver.restquery.query.SearchFilterExpr.{Atom, Dimension}

sealed trait SearchFilterExpr {
  def find(p: SearchFilterExpr => Boolean): Option[SearchFilterExpr]
  def replace(f: PartialFunction[SearchFilterExpr, SearchFilterExpr]): SearchFilterExpr

  def findEqFilter(fieldName: String): Option[SearchFilterExpr] =
    findEqFilter(Dimension(None, fieldName))

  def findEqFilter(dimension: Dimension): Option[SearchFilterExpr] = {
    this.find {
      case Atom.Binary(`dimension`, Eq, _) => true
      case _                               => false
    }
  }
}

object SearchFilterExpr {

  val Empty: Intersection = Intersection.Empty
  val Forbid = Atom.Binary(
    dimension = Dimension(None, "true"),
    op = SearchFilterBinaryOperation.Eq,
    value = "false"
  )

  final case class Dimension(tableName: Option[String], name: String) {
    def isForeign: Boolean = tableName.isDefined
  }

  sealed trait Atom extends SearchFilterExpr {
    override def find(p: SearchFilterExpr => Boolean): Option[SearchFilterExpr] = {
      Some(this).filter(p)
    }

    override def replace(f: PartialFunction[SearchFilterExpr, SearchFilterExpr]): SearchFilterExpr = {
      if (f.isDefinedAt(this)) f(this)
      else this
    }
  }

  object Atom {
    final case class Binary(dimension: Dimension, op: SearchFilterBinaryOperation, value: AnyRef) extends Atom
    object Binary {
      def apply(field: String, op: SearchFilterBinaryOperation, value: AnyRef): Binary =
        Binary(Dimension(None, field), op, value)
    }

    final case class NAry(dimension: Dimension, op: SearchFilterNAryOperation, values: Seq[AnyRef]) extends Atom
    object NAry {
      def apply(field: String, op: SearchFilterNAryOperation, values: Seq[AnyRef]): NAry =
        NAry(Dimension(None, field), op, values)
    }

    /** dimension.tableName extractor */
    object TableName {
      def unapply(value: Atom): Option[String] = value match {
        case Binary(Dimension(tableNameOpt, _), _, _) => tableNameOpt
        case NAry(Dimension(tableNameOpt, _), _, _)   => tableNameOpt
      }
    }
  }

  final case class Intersection private (operands: Seq[SearchFilterExpr])
      extends SearchFilterExpr with SearchFilterExprSeqOps {

    override def replace(f: PartialFunction[SearchFilterExpr, SearchFilterExpr]): SearchFilterExpr = {
      if (f.isDefinedAt(this)) f(this)
      else {
        this.copy(operands.map(_.replace(f)))
      }
    }

  }

  object Intersection {

    val Empty = Intersection(Seq())

    def create(operands: SearchFilterExpr*): SearchFilterExpr = {
      val filtered = operands.filterNot(SearchFilterExpr.isEmpty)
      filtered.size match {
        case 0 => Empty
        case 1 => filtered.head
        case _ => Intersection(filtered)
      }
    }
  }

  final case class Union private (operands: Seq[SearchFilterExpr])
      extends SearchFilterExpr with SearchFilterExprSeqOps {

    override def replace(f: PartialFunction[SearchFilterExpr, SearchFilterExpr]): SearchFilterExpr = {
      if (f.isDefinedAt(this)) f(this)
      else {
        this.copy(operands.map(_.replace(f)))
      }
    }

  }

  object Union {

    val Empty = Union(Seq())

    def create(operands: SearchFilterExpr*): SearchFilterExpr = {
      val filtered = operands.filterNot(SearchFilterExpr.isEmpty)
      filtered.size match {
        case 0 => Empty
        case 1 => filtered.head
        case _ => Union(filtered)
      }
    }

    def create(dimension: Dimension, values: String*): SearchFilterExpr = values.size match {
      case 0 => SearchFilterExpr.Empty
      case 1 => SearchFilterExpr.Atom.Binary(dimension, SearchFilterBinaryOperation.Eq, values.head)
      case _ =>
        val filters = values.map { value =>
          SearchFilterExpr.Atom.Binary(dimension, SearchFilterBinaryOperation.Eq, value)
        }

        create(filters: _*)
    }

    def create(dimension: Dimension, values: Set[String]): SearchFilterExpr =
      create(dimension, values.toSeq: _*)

    // Backwards compatible API

    /** Create SearchFilterExpr with empty tableName */
    def create(field: String, values: String*): SearchFilterExpr =
      create(Dimension(None, field), values: _*)

    /** Create SearchFilterExpr with empty tableName */
    def create(field: String, values: Set[String]): SearchFilterExpr =
      create(Dimension(None, field), values)
  }

  case object AllowAll extends SearchFilterExpr {
    override def find(p: SearchFilterExpr => Boolean): Option[SearchFilterExpr] = {
      Some(this).filter(p)
    }

    override def replace(f: PartialFunction[SearchFilterExpr, SearchFilterExpr]): SearchFilterExpr = {
      if (f.isDefinedAt(this)) f(this)
      else this
    }
  }

  case object DenyAll extends SearchFilterExpr {
    override def find(p: SearchFilterExpr => Boolean): Option[SearchFilterExpr] = {
      Some(this).filter(p)
    }

    override def replace(f: PartialFunction[SearchFilterExpr, SearchFilterExpr]): SearchFilterExpr = {
      if (f.isDefinedAt(this)) f(this)
      else this
    }
  }

  def isEmpty(expr: SearchFilterExpr): Boolean = {
    expr == Intersection.Empty || expr == Union.Empty
  }

  sealed trait SearchFilterExprSeqOps { this: SearchFilterExpr =>

    val operands: Seq[SearchFilterExpr]

    override def find(p: SearchFilterExpr => Boolean): Option[SearchFilterExpr] = {
      if (p(this)) Some(this)
      else {
        // Search the first expr among operands, which satisfy p
        // Is's ok to use foldLeft. If there will be performance issues, replace it by recursive loop
        operands.foldLeft(Option.empty[SearchFilterExpr]) {
          case (None, expr) => expr.find(p)
          case (x, _)       => x
        }
      }
    }
  }

}
