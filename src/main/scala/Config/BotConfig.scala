package subbot.config

import com.typesafe.config.ConfigFactory

object BotConfig {

  private val config = ConfigFactory.load()
  private val botConfig = config.getConfig("SubBot")

  object conn {
    private val fb = botConfig.getConfig("conn")
    val localhost: String = fb.getString("localhost")
    val port: Int = fb.getInt("port")
  }

  object fb {
    private val fb = botConfig.getConfig("fb")
    val pageAccessToken: String = fb.getString("pageAccessToken")
    val verifyToken: String = fb.getString("verifyToken")
    val responseUri: String = fb.getString("responseUri")
  }

  object db {
    private val db = botConfig.getConfig("db")
    val path: String = db.getString("path")
    val dbname: String = db.getString("dbname")
    val articles: String = db.getString("articles")
    val subscriptions: String = db.getString("subscriptions")

  }

  object texts {
    val subscribed = "Теперь вы подписанны на тег: "
    val unsubscribed = "Теперь вы не будете получать новостей по тегу: "
    val allTags = "Теги на которые вы подписаны: "
    val help =
      """tag/title <запрос> — поиск новостей по тегу / оглавлению
        |subscribe <тег> — подписаться на тег
        |unsubscribe <тег> — отписаться от тега
        |subscriptions — показать все подписки
        |help — это сообщение
      """.stripMargin
    val error = "Запрос не поддерживается :("
    val nothingFound = "По запросу ничего не найдено"
  }

  object crawler {
    private val crawler = botConfig.getConfig("crawler")
    val urlPostfix: String = crawler.getString("urlPostfix")
    val overflowRedirect: String = crawler.getString("overflowRedirect")
  }

}
