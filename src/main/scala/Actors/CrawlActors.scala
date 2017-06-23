package subbot.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.joda.time.format._
import org.joda.time.{DateTime, DateTimeZone}
import reactivemongo.bson.BSONString
import subbot.config.BotConfig.crawler._
import subbot.database.DBController
import subbot.json.fbmodel._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case object UpdateLastUrl

case object Tick

case object Crawl

case class Scrap(url: String)

case class Article(title: String,
                   description: String,
                   url: String,
                   image: String,
                   tags: List[String])


class Crawler(sender: ActorRef) extends Actor with ActorLogging {

  var lastLink: String = ""
  private val scraper = context actorOf Props(new Scraper(sender))

  def receive = {
    case UpdateLastUrl => DBController.lastArticle.map { lastUrl =>
      lastLink = lastUrl + urlPostfix
      log.info(s"Last url updated: $lastLink")
    }

    case Tick => {
      self ! UpdateLastUrl
      self ! Crawl
    }

    case Crawl => {
      implicit val browser = JsoupBrowser()

      val links = notParsedLinks

      links match {
        case Nil => log.info("No links to scrap")
        case list: List[String] => {
          list.foreach(scraper ! Scrap(_))
          log.info("Found " + list.length + " links to scrap")
        }
      }
    }
  }

  def notParsedLinks(implicit browser: Browser): List[String] = {
    def loop(pageNum: Int, acc: List[String]): List[String] = {
      val doc = browser.get(s"https://tproger.ru/amp/page/$pageNum")
      val links = doc >> elementList(".amp-wp-title a") >> attr("href")
      val canonical = doc >> attr("href")("link[rel=canonical]")

      val foundLast = links.contains(lastLink)
      val redirected = canonical == overflowRedirect

      (foundLast, redirected) match {
        case (true, _) => links.takeWhile(_ != lastLink) ++ acc
        case (_, true) => acc
        case _ => loop(pageNum + 1, links ++ acc)
      }
    }

    loop(1, Nil)
  }
}

class Scraper(sender: ActorRef) extends Actor {
  val browser = JsoupBrowser()
  private val Notifications = context actorOf Props(new Notificator(sender))

  def receive = {
    case Scrap(link) => {
      val doc = browser.get(link)


      val title = doc >> attr("content")("meta[property=og:title]")
      val description = doc >> attr("content")("meta[property=og:description]")
      val url = doc >> attr("content")("meta[property=og:url]")
      val image = doc >> attr("content")("meta[property=og:image]")

      val tags = doc >> elementList("meta[property=article:tag]") >> attr("content")
      val category = doc >> elementList("meta[property=article:section]") >> attr("content")
      val allTags = tags ++ category

      val time = doc >> attr("content")("meta[property=article:published_time]")
      val formatter: DateTimeFormatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.getDefault())

      val date: DateTime = new DateTime(time)
      Notifications ! Article(title, description, url, image, allTags)
      DBController.addArticle(title, description, url, image, allTags, date.getMillis)
    }
  }
}

class Notificator(sender: ActorRef) extends Actor {

  import subbot.actors.MessageCreators._

  def receive = {
    case Article(title, description, url, image, tags) => {
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
        sender ! Post(createCardMessage(card, id))
      }
    }
  }
}


