package subbot.json

package object fbmodel {

  case class Payload(url: Option[String] = None,
                     template_type: Option[String] = None,
                     top_element_style: Option[String] = None,
                     elements: Option[List[Card]] = None,
                     text: Option[String] = None,
                     buttons: Option[List[Button]] = None)

  case class Card(title: String,
                  subtitle: String,
                  image_url: String,
                  default_action: Option[DefaultAction] = None,
                  buttons: Option[List[Button]] = None)

 case class DefaultAction(`type`: String,
                          url: String,
                          fallback_url: Option[String] = None)

  case class Button(`type`: Option[String] = None,
                    url: Option[String] = None,
                    title: String,
                    payload: Option[String] = None)



  case class Attachment(`type`: String, payload: Payload)

  case class FBMessage(mid: Option[String] = None,
                       seq: Option[Long] = None,
                       text: Option[String] = None,
                       metadata: Option[String] = None,
                       attachment: Option[Attachment] = None)

  case class FBSender(id: String)

  case class FBRecipient(id: String)

  case class FBMessageEventIn(recipient: FBRecipient,
                              sender: FBSender,
                              timestamp: Long,
                              message: Option[FBMessage] = None,
                              postback: Option[FBPostback] = None)

  case class FBPostback(payload: String)

  case class FBMessageEventOut(recipient: FBRecipient,
                               message: FBMessage)

  case class FBEntry(id: String, time: Long, messaging: List[FBMessageEventIn])

  case class FBPObject(`object`: String, entry: List[FBEntry])

}
