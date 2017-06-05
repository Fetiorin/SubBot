package subbot.actors

import reactivemongo.bson.{BSONString, BSONValue}
import subbot.json.fbJson._
import spray.json._
import subbot.json.fbmodel._

package object MessageCreators {

  def makeCard(article: Map[String, BSONValue]): Card = {
    val BSONString(title) = article("title")
    val BSONString(subtitle) = article("description")
    val BSONString(image_url) = article("image")
    val BSONString(url) = article("url")
    Card(
      title,
      subtitle,
      image_url,
      default_action = Some(DefaultAction(
        `type` = "web_url",
        url
      ))
    )
  }

  def createMoreButton(payload: String, id: String) =
    toBytesWrapper(
      id = id,
      message = FBMessage(attachment = Some(
        Attachment(`type` = "template",
          Payload(
            text = Some("Показать ещё?"),
            template_type = Some("button")
            ,
            buttons = Some(List(
              Button(`type` = Some("postback"),
                title = "Да",
                payload = Some(payload))))
          ))))
    )


  def createCardMessage(card: Card, id: String) = toBytesWrapper(
    id = id,
    message = FBMessage(attachment = Some(
      Attachment(`type` = "template",
        Payload(
          template_type = Some("generic"),
          elements = Some(
            List(card))))))
  )

  def createListMessage(cards: List[Card], payload: String, id: String) =
    toBytesWrapper(
      id = id,
      message = FBMessage(attachment = Some(
        Attachment(`type` = "template",
          Payload(template_type = Some("generic"), elements = Some(cards))))))


  def createMessage(text: String, id: String) = toBytesWrapper(
    id = id,
    message = FBMessage(text = Some(s"$text"), metadata = None)
  )

  def toBytesWrapper(id: String, message: FBMessage) =
    FBMessageEventOut(recipient = FBRecipient(id),
      message = message).toJson.toString().getBytes
}
