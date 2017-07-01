import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import subbot.config.BotConfig
import subbot.database.DBController
import subbot.server.FbHttpConnection
import subbot.utils.Article
import subbot.utils.ScrapUtils._

import scala.concurrent.duration._

object SubBotApp extends App with LazyLogging {
  val db = DBController

  implicit val actorSystem = ActorSystem("SubBot", ConfigFactory.load)
  implicit val materializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global

  //creates Route and
  val fbConnection = new FbHttpConnection()
  //possible to add some user-input as a trigger
  val ScrapTriggers = Source.tick(60 seconds, 2 minutes, NotUsed)

  ScrapTriggers.runForeach { _ =>
    Source(notParsedLinks) //add throttle for slow connection
      .map(scrap)
      .map { article: Article =>
        fbConnection.notifyUsers(article)
        db.addArticle(article)
      }.runForeach { _ => logger.info("Article scrapped") }
  }

  Http().bindAndHandle(fbConnection.fbRoute, BotConfig.conn.localhost, BotConfig.conn.port)

  logger.info(s"Subbot started on: ${BotConfig.conn.localhost}")
}
