package suite

import com.typesafe.config.ConfigFactory
import org.scalatest._
import play.api.{Application, Configuration}
import reactivemongo.api._
import repositories.{CacheMapRepository, Repository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls

object MongoSuite {

  private lazy val config = Configuration(ConfigFactory.defaultApplication().resolve())

  private lazy val parsedUri = Future.fromTry {
    MongoConnection.parseURI(config.getString("mongodb.uri").get)
  }

  lazy val connection =
    parsedUri.map(MongoDriver().connection)
}

trait MongoSuite {
  self: TestSuite =>

  def started(repositories: Repository*): Future[_] = {

    val started = repositories.map(_.started)

    Future.sequence(started)
  }

  def database: Future[DefaultDB] = {
    for {
      uri        <- MongoSuite.parsedUri
      connection <- MongoSuite.connection
      database   <- connection.database(uri.db.get)
    } yield database
  }
}