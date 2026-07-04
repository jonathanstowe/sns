package me.snov.sns.model

import spray.json._

case class Topic(arn: String,name: String)

object Topic extends DefaultJsonProtocol {
  implicit val format: RootJsonFormat[Topic] = jsonFormat2(Topic.apply)
}
