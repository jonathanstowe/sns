package me.snov.sns.model

import spray.json._

case class Subscription(
                         arn: String,
                         owner: String,
                         topicArn: String,
                         protocol: String,
                         endpoint: String,
                         subscriptionAttributes: Option[Map[String, String]] = None
                       ) {

  def isRawMessageDelivery: Boolean = {
    subscriptionAttributes.getOrElse(Map.empty[String, String]).get("RawMessageDelivery").exists(java.lang.Boolean.parseBoolean)
  }
}

object Subscription extends DefaultJsonProtocol {
  implicit val format: RootJsonFormat[Subscription] = jsonFormat6(Subscription.apply)
}
