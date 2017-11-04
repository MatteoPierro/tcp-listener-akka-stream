import Listener.{MessageHandler, echo}

class EchoWithNotification(val notificationService: NotificationService) extends MessageHandler {

  override def apply(id: Int) = {
    val (subscriber, subscriberSource) = notificationService.subscribe(id)

    subscriber ! "Welcome"
    subscriber ! s"send notification to http://localhost:8080/notify/$id"

    echo.merge(subscriberSource)
  }
}
