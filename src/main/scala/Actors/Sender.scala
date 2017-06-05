package subbot.actors


import akka.actor.Actor
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import subbot.database.DBController
import subbot.server.FBServer
import subbot.config.BotConfig
import subbot.actors.MessageCreators._

import scala.util.matching.Regex

case class Message(text: String, id: String)

case class More(postback: String, id: String)

case class MoreButton(poyload: String, id: String)

case class Post(bytes: Array[Byte])


class Sender(implicit server: FBServer) extends Actor {

  def receive = {
    case Message(text, id) =>
      identifyMessage(text, id)
    case More(postback, id) =>
      val Array(typ, query, date) = postback.split("&")
      typ match {
        case "tag" =>
          searchAndReplay(query, id, DBController.searchInPast(date.toLong, DBController.tagQuery), typ)
        case "title" =>
          searchAndReplay(query, id, DBController.searchInPast(date.toLong, DBController.titleQuery), typ)
      }
    case Post(bytes) => server.post(bytes)
  }

  def searchAndReplay(query: String,
                      id: String,
                      search: (String => Future[List[BSONDocument]]),
                      typ: String) = search(query).map {
    docs => {
      docs match {
        case Nil =>
          server.post(createMessage(BotConfig.texts.nothingFound, id))
        case doc =>
          val articles = doc.map { article => makeCard(article.toMap) }
          val BSONDateTime(date) = docs.last.toMap("date")
          val payload = List(typ, query, date) mkString "&"
          articles.length match {
            case n if n == 10 =>
              server.post(createListMessage(articles, payload, id))
                .map(_ => server.post(createMoreButton(payload, id)))
            case _ =>
              server.post(createListMessage(articles, payload, id))
          }
      }
    }
  }

  def identifyMessage(text: String, id: String) = {
    val reg = """^([^\s]+)\s{0,1}(.*)$""".r
    val reg(prefix, suffix) = text

    (prefix, suffix) match {
      case ("tag", tag) =>
        searchAndReplay(tag, id, DBController.searchByTag, "tag")

      case ("title", title) =>
        searchAndReplay(title, id, DBController.searchByTitle, "title")

      case ("subscribe", tag) =>
        DBController.subscribe(id, tag.toLowerCase)
        server.post(createMessage(BotConfig.texts.subscribed + tag, id))

      case ("unsubscribe", tag) =>
        DBController.unsubscribe(id, tag.toLowerCase)
        server.post(createMessage(BotConfig.texts.unsubscribed + tag, id))

      case ("subscriptions", _) =>
        DBController.getSubscriptions(id) map { subs =>
          val tags: List[String] = subs.map { x => val BSONString(a) = x.get("tag").get; a }
          server.post(createMessage(tags.mkString(start = BotConfig.texts.allTags,
            sep = ", ",
            end = "."), id))
        }
      case ("help", _) => server.post(createMessage(BotConfig.texts.help, id))
      case _ => server.post(createMessage(BotConfig.texts.error, id))
    }
  }
}

