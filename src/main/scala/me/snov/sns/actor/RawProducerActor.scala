package me.snov.sns.actor

import me.snov.sns.model.Message
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.apache.pekko.actor.{Actor, ActorLogging, Props}

object RawProducerActor {
  def props(endpoint: String, subscriptionArn: String, topicArn: String) = Props(classOf[RawProducerActor], endpoint, subscriptionArn, topicArn)
}

class RawProducerActor(endpoint: String, subscriptionArn: String, topicArn: String) extends Actor with ActorLogging {
  // Use Apache Camel ProducerTemplate directly instead of akka-camel
  private val camelContext = new DefaultCamelContext()
  private val producer: ProducerTemplate = {
    camelContext.start()
    camelContext.createProducerTemplate()
  }

  override def postStop(): Unit = {
    try producer.stop() catch { case _: Throwable => () }
    try camelContext.stop() catch { case _: Throwable => () }
  }

  override def receive: Receive = {
    case snsMsg: Message =>
      val body = snsMsg.bodies.getOrElse("default", "")
      val headers = new java.util.HashMap[String, Object]()
      headers.put("x-amz-sns-message-type", "Notification")
      headers.put("x-amz-sns-message-id", snsMsg.uuid.toString)
      headers.put("x-amz-sns-subscription-arn", subscriptionArn)
      headers.put("x-amz-sns-topic-arn", topicArn)

      // add message attributes as headers
      snsMsg.messageAttributes.foreach { case (k, v) => headers.put(k, v.stringValue.asInstanceOf[Object]) }

      producer.sendBodyAndHeaders(endpoint, body, headers)
  }
}
