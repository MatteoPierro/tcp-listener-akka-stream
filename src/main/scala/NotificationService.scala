import Main.system
import NotificationService.{Notify, Subscribe}
import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import akka.event.EventStream
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}

case class Notification(identifier: Int, message: String)

object NotificationService {

  case class Subscribe(id: Int, subscriber: ActorRef)

  case class Notify(notification: Notification)

}

class NotificationService(val actorSystem: ActorSystem) (implicit materializer: ActorMaterializer) {

  private val eventStream: EventStream = system.eventStream
  private val notificationActor = system.actorOf(Props(new NotificationActor(eventStream)))

  def subscribe(id: Int): (ActorRef, Source[String, NotUsed]) = {
    val bufferSize = 1000
    val (subscriber, subscriberPublisher) = Source.actorRef[String](bufferSize, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(fanout = false))(Keep.both)
      .run()

    notificationActor ! Subscribe(id, subscriber)

    (subscriber, Source.fromPublisher(subscriberPublisher))
  }

  def publish(notification: Notification): Unit = notificationActor ! Notify(notification)
}

private class NotificationActor(val eventStream: EventStream) extends Actor {

  override def receive: Receive = {
    case Subscribe(id, subscriber) =>
      val subscription = context.actorOf(Props(new Subscription(id, subscriber)))
      eventStream.subscribe(subscription, classOf[Notification])

    case Notify(notification) =>
      eventStream.publish(notification)
  }
}

private class Subscription(val id: Int, val subscriber: ActorRef) extends Actor {

  context.watch(subscriber)

  override def receive: Receive = {
    case notification: Notification if notification.identifier == id =>
      subscriber ! notification.message

    case Terminated(actor) if actor == subscriber =>
      self ! PoisonPill
  }
}