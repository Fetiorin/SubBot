package subbot.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._

import org.joda.time.format._
import org.joda.time.{DateTime, DateTimeZone}
import reactivemongo.bson.BSONString
import subbot.database.DBController
import subbot.json.fbmodel._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future



case object Init

case object Tick

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
    case Init => DBController.lastArticle.map { x =>
      val BSONString(s) = x
      lastLink = s + "amp/"
      log.info("Crawler inited")
    }
    case Tick => {
      val browser = JsoupBrowser()
      val doc = browser.get(s"https://tproger.ru/amp/")
      val links = (doc >> elementList(".amp-wp-title a") >> attr("href")).takeWhile(_ != lastLink)
      links match {
        case Nil => log.info("No links to scrapp")
        case list @ (x :: _) => {
          lastLink = x
          list.foreach(scraper ! Scrap(_))
          log.info("Found "+ list.length + " links to scrapp")
        }
      }
    }
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


