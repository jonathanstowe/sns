package me.snov.sns.actor

import org.apache.pekko.actor.{Actor, ActorLogging, Props}
import spray.json._
import me.snov.sns.model.Message
import org.apache.camel.Exchange
import org.apache.camel.component.http.HttpMethods
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.ProducerTemplate

object ProducerActor {
  def props(endpoint: String, subscriptionArn: String, topicArn: String) = Props(classOf[ProducerActor], endpoint, subscriptionArn, topicArn)
}

class ProducerActor(endpoint: String, subscriptionArn: String, topicArn: String) extends Actor with ActorLogging {
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
      val headers = new java.util.HashMap[String, Object]()
      // set headers similar to previous implementation
      headers.put(Exchange.HTTP_METHOD, HttpMethods.POST.asInstanceOf[Object])
      headers.put("x-amz-sns-message-type", "Notification")
      headers.put("x-amz-sns-message-id", snsMsg.uuid)
      headers.put("x-amz-sns-subscription-arn", subscriptionArn)
      headers.put("x-amz-sns-topic-arn", topicArn)

      producer.sendBodyAndHeaders(endpoint, snsMsg.toJson.compactPrint, headers)
  }
}
