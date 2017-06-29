package subbot.database

import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver, QueryOpts}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONArray, BSONDateTime, BSONDocument, BSONRegex, BSONString}
import subbot.config.BotConfig

import scala.concurrent.Future

object DBController {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val location = BotConfig.db.path
  private val dbname = BotConfig.db.dbname

  val db: DefaultDB = createConnection(location, dbname)

  val subscriptions: BSONCollection = getCollection(BotConfig.db.subscriptions)
  val articles: BSONCollection = getCollection(BotConfig.db.articles)

  def getCollection(name: String): BSONCollection =
    db.collection(name)

  def getSubscriptions(user: String): Future[List[BSONDocument]] = {
    subscriptions
      .find(BSONDocument("user" -> user))
      .cursor[BSONDocument]().toList()
  }

  private def search(query: BSONDocument): Future[List[BSONDocument]] = {
    articles
      .find(query)
      .options(QueryOpts().batchSize(10))
      .sort(BSONDocument("date" -> -1))
      .cursor[BSONDocument]()
      .collect[List](10)
  }

  def searchByTag(tag: String): Future[List[BSONDocument]] =
    search(tagQuery(tag))

  def searchByTitle(title: String): Future[List[BSONDocument]] =
    search(titleQuery(title))

  def searchInPast(time: Long,
                   queryCreator: String => BSONDocument)(query: String): Future[List[BSONDocument]] = search {
    BSONDocument(BSONDocument("date"
      -> BSONDocument("$lt"
      -> BSONDateTime(time))
    ),
      queryCreator(query)
    )
  }

  def whoSubscribed(tag: String): Future[Set[BSONDocument]] = subscriptions.find(
    BSONDocument(
      "tag" -> BSONRegex(s"$tag", "i")),
    BSONDocument(
      "user" -> 1,
      "_id" -> 0
    )
  ).cursor[BSONDocument]().collect[Set]()

  def addArticle(title: String,
                 description: String,
                 url: String,
                 image: String,
                 tags: List[String],
                 date: Long) = articles.insert(
    BSONDocument(
      "title" -> title,
      "description" -> description,
      "url" -> url,
      "image" -> image,
      "tags" -> tags,
      "date" -> BSONDateTime(date)
    )
  )

  def subscribe(user: String, tag: String) =
    subscriptions.insert(BSONDocument("user" -> user, "tag" -> tag))


  def unsubscribe(user: String, tag: String): Future[WriteResult] =
    subscriptions.remove(BSONDocument("user" -> user, "tag" -> tag))

  def lastArticle: Future[String] = {
    articles.find(BSONDocument()).
      sort(BSONDocument("date" -> -1)).
        one[BSONDocument].map { article =>
          val url = article.flatMap(_.get("url"))
          url match {
            case Some(BSONString(value)) => value
            case _ => ""
          }
        }
  }

  private def createConnection(location: String, dbname: String): DefaultDB = {
    val driver = new MongoDriver
    val connection = driver.connection(MongoConnection.parseURI(location).get)

    connection(dbname)
  }

  def tagQuery(tag: String) = BSONDocument("tags" ->
    BSONDocument("$in" ->
      BSONArray(BSONRegex(s"^$tag$$", "i"))))

  def titleQuery(title: String) = BSONDocument("title" -> BSONRegex(s"$title", "i"))
}
