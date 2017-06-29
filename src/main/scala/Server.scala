package subbot.server


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import subbot.utils.SenderUtils._
import subbot.config.BotConfig.fb
import subbot.config._
import subbot.json.fbJson._
import subbot.json.fbmodel._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait implicits {
  implicit val actorSystem = ActorSystem("SubBot", ConfigFactory.load)
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
}

object FBServer extends LazyLogging with implicits {

  def post(body: Array[Byte]): Future[Unit] = {
    val entity = HttpEntity(MediaTypes.`application/json`, body)
    val response: Future[HttpResponse] = Http().singleRequest(HttpRequest(HttpMethods.POST, Uri(s"${fb.responseUri}?access_token=${fb.pageAccessToken}"), entity = entity))
    val result = response.flatMap {
      response =>
        response.status match {
          case status if status.isSuccess =>
            Future.successful()
          case _ =>
            Future.successful(throw new Exception(s"Error: ${response.entity}"))
        }
    }
    result.onComplete {
      case Success(response) =>
        //logger.info(s"Successful response: $response")
      case Failure(ex) =>
        logger.info(s"Response failed: $ex" )
    }
    result
  }


  def verifyToken(token: String, mode: String, challenge: String):
  (StatusCode, List[HttpHeader], Option[String]) = {
    if (mode == "subscribe" && token == BotConfig.fb.verifyToken) {
      (StatusCodes.OK, List.empty[HttpHeader], Some(challenge))
    }
    else {
      (StatusCodes.Forbidden, List.empty[HttpHeader], None)
    }
  }


  def handleMessage(fbObject: FBPObject):
  (StatusCode, List[HttpHeader], Option[Either[String, String]]) = {
    logger.info(s"Receive fbObject: $fbObject")
    fbObject.entry.foreach {
      entry =>
        entry.messaging.foreach {
            case FBMessageEventIn(_, senderId,_,_,Some(postback)) =>
              moreArticles(postback.payload, senderId.id)
            case FBMessageEventIn(_, senderId, _, Some(senderMessage), _) =>
              senderMessage.text match {
                case Some(text) =>
                  replyOnMessage(text, senderId.id)
                case None =>
                  Future.successful(())
              }
            case _ =>
        }
    }

    (StatusCodes.OK, List.empty[HttpHeader], None)
  }

}

trait FBRoute extends LazyLogging with implicits {
  val fbRoutes: Route = {
    get {
      path("webhook") {
        parameters("hub.verify_token", "hub.mode", "hub.challenge") {
          (token, mode, challenge) =>
            complete(FBServer.verifyToken(token, mode, challenge))
        }
      }
    } ~
      post {
        path("webhook") {
          entity(as[FBPObject]) { fbObject =>
            complete {
              FBServer.handleMessage(fbObject)
            }
          }
        }
      }
  }
}

