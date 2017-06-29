package subbot.utils

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.joda.time.format._
import org.joda.time.{DateTime, DateTimeZone}
import subbot.config.BotConfig.crawler._
import subbot.database.DBController

import scala.concurrent.Await
import scala.concurrent.duration._

case class Article(title: String,
                   description: String,
                   url: String,
                   image: String,
                   tags: List[String],
                   date: DateTime)

object ScrapUtils {

  def notParsedLinks: List[String] = {
    val lastParsedLink = Await.result(DBController.lastArticle, 1 seconds) + urlPostfix
    val browser = JsoupBrowser()

    def loop(pageNum: Int, acc: List[String]): List[String] = {
      val doc = browser.get(s"https://tproger.ru/amp/page/$pageNum")
      val links = doc >> elementList(".amp-wp-title a") >> attr("href")
      val canonical = doc >> attr("href")("link[rel=canonical]")
      val foundLast = links.contains(lastParsedLink)
      val redirected = canonical == overflowRedirect
      (foundLast, redirected) match {
        case (true, _) => links.takeWhile(_ != lastParsedLink) ++ acc
        case (_, true) => acc
        case _ => loop(pageNum + 1, links ++ acc)
      }
    }
    loop(1, Nil)
  }

  def scrap(link: String): Article = {
    val browser = JsoupBrowser()
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

    Article(title, description, url, image, allTags, date)
  }

  def notifyUsers(article: Article) = SenderUtils.sendNotifications(article)

  def addToDB(article: Article) = {
    val Article(title, description, url, image, tags, date) = article
    DBController.addArticle(title, description, url, image, tags, date.getMillis)
  }
}
