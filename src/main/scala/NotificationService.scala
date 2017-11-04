import NotificationService.{Notify, Subscribe}
import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import akka.event.EventStream

case class Notification(identifier: Int, message: String)

object NotificationService {
  case class Subscribe(id: Int, subscriber: ActorRef)
  case class Notify(notification: Notification)
}

class NotificationService(eventStream: EventStream) extends Actor {
  override def receive: Receive = {
    case Subscribe(id, subscriber) =>
      val subscription = context.actorOf(Props(new Subscription(id, subscriber)))
      eventStream.subscribe(subscription, classOf[Notification])

    case Notify(notification) =>
      eventStream.publish(notification)
  }
}

class Subscription(id: Int, subscriber: ActorRef) extends Actor {
  context.watch(subscriber)

  override def receive: Receive = {
    case notification: Notification  if notification.identifier == id =>
      subscriber ! notification.message

    case Terminated(actor) if actor == subscriber =>
      self ! PoisonPill
  }
}