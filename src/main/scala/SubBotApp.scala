import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import subbot.actors.{Crawler, Init, Sender, Tick}
import subbot.config.BotConfig
import subbot.database.DBController
import subbot.server.{FBRoute, FBServer}

import scala.concurrent.duration._

object SubBotApp extends App with FBRoute {
  val db = DBController

  implicit val actorSystem = ActorSystem("SubBot", ConfigFactory.load)
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val server = new FBServer
  implicit val sender = actorSystem.actorOf(Props(new Sender))

  val crawler = actorSystem.actorOf(Props(new Crawler(sender)))

  crawler ! Init
  actorSystem.scheduler.schedule(1 minutes, 10 minutes, crawler, Tick)

  Http().bindAndHandle(fbRoutes, BotConfig.conn.localhost, BotConfig.conn.port)

  logger.info(s"Subbot started on: ${BotConfig.conn.localhost}")
}
