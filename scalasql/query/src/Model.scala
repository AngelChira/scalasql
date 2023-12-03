package scalasql.query

import scalasql.core.Sql

/**
 * Models a SQL `ORDER BY` clause
 */
case class OrderBy(expr: Sql[_], ascDesc: Option[AscDesc], nulls: Option[Nulls])

sealed trait AscDesc

object AscDesc {

  /**
   * Models a SQL `ASC` clause
   */
  case object Asc extends AscDesc

  /**
   * Models a SQL `DESC` clause
   */
  case object Desc extends AscDesc
}

sealed trait Nulls

object Nulls {

  /**
   * Models a SQL `NULLS FIRST` clause
   */
  case object First extends Nulls

  /**
   * Models a SQL `NULSL LAST` clause
   */
  case object Last extends Nulls
}

/**
 * Models a SQL `GROUP BY` clause
 */
case class GroupBy(key: Sql[_], select: () => Select[_, _], having: Seq[Sql[_]])

/**
 * Models a SQL `JOIN` clause
 */
case class Join(prefix: String, from: Seq[Join.From])
object Join {
  case class From(from: scalasql.core.Context.From, on: Option[Sql[_]])
}
