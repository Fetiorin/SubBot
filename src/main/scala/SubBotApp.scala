import akka.NotUsed
import akka.stream.scaladsl.Source
import subbot.config.BotConfig
import subbot.database.DBController
import subbot.server.FBRoute
import subbot.utils.Article
import subbot.utils.ScrapUtils._

import scala.concurrent.duration._

object SubBotApp extends App with FBRoute {
  val db = DBController

  //possible to add some user-input as a trigger
  val ScrapTriggers = Source.tick(10 seconds, 40 seconds, NotUsed)

  ScrapTriggers.runForeach { _ =>
    Source(notParsedLinks)
      .map(scrap)
      .map { article: Article =>
        notifyUsers(article)
        addToDB(article)
      }.runForeach { _ => logger.info("Article scrapped") }
  }

  //Http().bindAndHandle(fbRoutes, BotConfig.conn.localhost, BotConfig.conn.port)

  logger.info(s"Subbot started on: ${BotConfig.conn.localhost}")
}
