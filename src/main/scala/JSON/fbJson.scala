package subbot.json

import spray.json.DefaultJsonProtocol
import subbot.json.fbmodel._

/**
  * Describes Facebook JSON formats in spray JsonProtocol, see fbModel for details
  * of objects structure
  */

object fbJson extends DefaultJsonProtocol {

  implicit val buttonFormat = jsonFormat4(Button)
  implicit val defaultAction = jsonFormat3(DefaultAction)
  implicit val cardFormat = jsonFormat5(Card)
  implicit val payloadFormat = jsonFormat6(Payload)
  implicit val fbpostbackFormat = jsonFormat1(FBPostback)
  implicit val attachmentFormat = jsonFormat2(Attachment)
  implicit val fbMessageFormat = jsonFormat5(FBMessage)
  implicit val fbSenderFormat = jsonFormat1(FBSender)
  implicit val fbRecipientFormat = jsonFormat1(FBRecipient)
  implicit val fbMessageObjectFormat = jsonFormat5(FBMessageEventIn)
  implicit val fbMessageEventOutFormatOut = jsonFormat2(FBMessageEventOut)
  implicit val fbEntryFormat = jsonFormat3(FBEntry)
  implicit val fbPObjectFormat = jsonFormat2(FBPObject)

}

