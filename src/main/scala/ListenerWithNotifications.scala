import NotificationService.{Notify, Subscribe}
import akka.actor.{ActorSystem, Props}
import akka.event.EventStream
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString

object ListenerWithNotifications extends App {

  implicit val system: ActorSystem = ActorSystem("Listener")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val eventStream: EventStream = system.eventStream
  val notificationService = system.actorOf(Props(new NotificationService(eventStream)))

  import akka.stream.scaladsl.Framing

  val connections = Tcp().bind("127.0.0.1", 9999)

  connections runForeach { connection =>
    println(s"New connection from: ${connection.remoteAddress}")

    val (notificationActor, notificationPublisher) = Source.actorRef[String](1000, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(fanout = false))(Keep.both)
      .run()

    val id = connection.remoteAddress.getPort
    notificationService ! Subscribe(id, notificationActor)

    notificationActor ! "Welcome"
    notificationActor ! s"send notification to http://localhost:8080/notify/$id"

    val notificationSource = Source.fromPublisher(notificationPublisher)

    val echoWithNotification = Flow[ByteString]
      .via(Framing.delimiter(
        ByteString("\n"),
        maximumFrameLength = 256,
        allowTruncation = true))
      .map(_.utf8String)
      .merge(notificationSource)
      .map(_ + "\n")
      .map(ByteString(_))

    connection.handleWith(echoWithNotification)
  }
  
  Http().bindAndHandle(NotificationRoute(notificationService), "localhost", 8080)
}
