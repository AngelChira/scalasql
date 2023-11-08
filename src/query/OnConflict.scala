package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{Column, MappedType}
import scalasql.renderer.{Context, SqlStr}

/**
 * A query with a SQL `ON CONFLICT` clause, typically an `INSERT` or an `UPDATE`
 */
class OnConflict[Q, R](query: Query[R] with InsertReturnable[Q], expr: Q, table: TableRef) {
  def onConflictIgnore(c: (Q => Column.ColumnExpr[_])*) =
    new OnConflict.Ignore(query, c.map(_(expr)), table)
  def onConflictUpdate(c: (Q => Column.ColumnExpr[_])*)(c2: (Q => Column.Assignment[_])*) =
    new OnConflict.Update(query, c.map(_(expr)), c2.map(_(expr)), table)
}

object OnConflict {
  class Ignore[Q, R](
      query: Query[R] with InsertReturnable[Q],
      columns: Seq[Column.ColumnExpr[_]],
      val table: TableRef
  ) extends Query[R]
      with InsertReturnable[Q] {
    def expr = query.expr
    def walk() = query.walk()
    def singleRow = query.singleRow
    def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      val (str, mapped) = query.toSqlQuery
      (
        str +
          sql" ON CONFLICT (${SqlStr.join(columns.map(c => SqlStr.raw(c.name)), sql", ")}) DO NOTHING",
        mapped
      )
    }

    override def isExecuteUpdate = true

    def valueReader = query.valueReader

  }

  class Update[Q, R](
      query: Query[R] with InsertReturnable[Q],
      columns: Seq[Column.ColumnExpr[_]],
      updates: Seq[Column.Assignment[_]],
      val table: TableRef
  ) extends Query[R]
      with InsertReturnable[Q] {
    def expr = query.expr
    def walk() = query.walk()
    def singleRow = query.singleRow
    def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = toSqlQuery0(ctx)
    def toSqlQuery0(ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      val computed = Context.compute(ctx, Nil, Some(table))
      import computed.implicitCtx
      val (str, mapped) = query.toSqlQuery
      val columnsStr = SqlStr.join(columns.map(c => SqlStr.raw(c.name)), sql", ")
      val updatesStr = SqlStr.join(
        updates.map { case assign => SqlStr.raw(assign.column.name) + sql" = ${assign.value}" },
        sql", "
      )
      (str + sql" ON CONFLICT (${columnsStr}) DO UPDATE SET $updatesStr", mapped)
    }
    override def isExecuteUpdate = true
    def valueReader = query.valueReader
  }
}
