package subbot.utils

import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONString}
import subbot.config.BotConfig
import subbot.database.DBController
import subbot.json.fbmodel.{Card, DefaultAction}
import subbot.server.FBServer
import subbot.utils.MessageCreators._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SenderUtils {
  def sendNotifications(article: Article) = {
    val Article(title, description, url, image, tags, _) = article
    val subscription = tags.map(tag => DBController.whoSubscribed(tag))
    val users = Future.reduce(subscription)(_ ++ _)
    for {set <- users
         user <- set} {
      val BSONString(id) = user.get("user").get
      val card = Card(title = title,
        subtitle = description,
        image_url = image,
        default_action = Some(DefaultAction(
          `type` = "web_url",
          url = url
        )))
      FBServer.post(createCardMessage(card, id))
    }
  }

  def moreArticles(postback: String, id: String) = {
    val Array(typ, query, date) = postback.split("&")
    typ match {
      case "tag" =>
        searchAndReply(query, id, DBController.searchInPast(date.toLong, DBController.tagQuery), typ)
      case "title" =>
        searchAndReply(query, id, DBController.searchInPast(date.toLong, DBController.titleQuery), typ)
    }
  }

  def replyOnMessage(text: String, id: String) = {
    val reg = """^([^\s]+)\s{0,1}(.*)$""".r
    val reg(prefix, suffix) = text

    (prefix, suffix) match {
      case ("tag", tag) =>
        searchAndReply(tag, id, DBController.searchByTag, "tag")

      case ("title", title) =>
        searchAndReply(title, id, DBController.searchByTitle, "title")

      case ("subscribe", tag) =>
        DBController.subscribe(id, tag.toLowerCase)
        FBServer.post(createMessage(BotConfig.texts.subscribed + tag, id))

      case ("unsubscribe", tag) =>
        DBController.unsubscribe(id, tag.toLowerCase)
        FBServer.post(createMessage(BotConfig.texts.unsubscribed + tag, id))

      case ("subscriptions", _) =>
        DBController.getSubscriptions(id) map { subs =>
          val tags: List[String] = subs.map { x => val BSONString(a) = x.get("tag").get; a }
          FBServer.post(createMessage(tags.mkString(start = BotConfig.texts.allTags,
            sep = ", ",
            end = "."), id))
        }
      case ("help", _) => FBServer.post(createMessage(BotConfig.texts.help, id))
      case _ => FBServer.post(createMessage(BotConfig.texts.error, id))
    }
  }

  private def searchAndReply(query: String,
                             id: String,
                             search: (String => Future[List[BSONDocument]]),
                             typ: String) = search(query).map {
    docs => {
      docs match {
        case Nil =>
          FBServer.post(createMessage(BotConfig.texts.nothingFound, id))
        case doc =>
          val articles = doc.map { article => makeCard(article.toMap) }
          val BSONDateTime(date) = docs.last.toMap("date")
          val payload = List(typ, query, date) mkString "&"
          articles.length match {
            case n if n == 10 =>
              FBServer.post(createListMessage(articles, payload, id))
                .map(_ => FBServer.post(createMoreButton(payload, id)))
            case _ =>
              FBServer.post(createListMessage(articles, payload, id))
          }
      }
    }
  }
}
