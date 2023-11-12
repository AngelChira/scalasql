package scalasql.example

import scalasql.Table
import java.sql.DriverManager
import scalasql.dialects.H2Dialect._


object H2Example {

  case class Product[+T[_]](id: T[Int],
                            kebabCaseName: T[String],
                            name: T[String],
                            price: T[Double])

  object Product extends Table[Product] {
    val metadata = initMetadata()
  }
  
  // The example H2 database comes from the library `com.h2database:h2:2.2.224`
  lazy val h2Client = new scalasql.DatabaseClient.Connection(
    DriverManager.getConnection("jdbc:h2:mem:mydb"),
    dialectConfig = scalasql.dialects.H2Dialect,
    config = new scalasql.Config {}
  )
  
  def main(args: Array[String]): Unit = {
    val db = h2Client.getAutoCommitClientConnection
    db.runRawUpdate("""
    |CREATE TABLE product (
    |    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    |    kebab_case_name VARCHAR(256),
    |    name VARCHAR(256),
    |    price DECIMAL(20, 2)
    |);
    |""".stripMargin)
    
    val inserted = db.run(
      Product.insert.batched(_.kebabCaseName, _.name, _.price)(
          ("face-mask", "Face Mask", 8.88),
          ("guitar", "Guitar", 300),
          ("socks", "Socks", 3.14),
          ("skate-board", "Skate Board", 123.45),
          ("camera", "Camera", 1000.00),
          ("cookie", "Cookie", 0.10)
        )
    )

    assert(inserted == 6)

    val expensive = db.run(Product.select.filter(_.price > 10).sortBy(_.price).desc.map(_.name))

    assert(expensive == Seq("Camera", "Guitar", "Skate Board"))

    db.run(Product.update(_.name === "Cookie").set(_.price := 11.0))

    val result2 = db.run(Product.select.filter(_.price > 10).sortBy(_.price).desc.map(_.name))

    assert(result2 == Seq("Camera", "Guitar", "Skate Board", "Cookie"))

    db.run(Product.delete(_.name === "Guitar"))

    val result3 = db.run(Product.select.filter(_.price > 10).sortBy(_.price).desc.map(_.name))

    assert(result3 == Seq("Camera", "Skate Board", "Cookie"))
  }
}
