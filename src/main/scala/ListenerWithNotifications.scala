import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString

object ListenerWithNotifications extends App {

  implicit val system = ActorSystem("Listener")
  implicit val materializer = ActorMaterializer()


  import akka.stream.scaladsl.Framing

  val connections = Tcp().bind("127.0.0.1", 9999)

  connections runForeach { connection =>
    println(s"New connection from: ${connection.remoteAddress}")

    val (notificationActor, notificationPublisher) = Source.actorRef[String](1000, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(fanout = false))(Keep.both)
      .run()

    system.eventStream.subscribe(notificationActor, classOf[String])
    notificationActor ! "Welcome"

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

  import akka.http.scaladsl.server.Directives._

  val route =
    path("hello") {
      get {
        system.eventStream.publish("hello from web")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Sent hello to tcp sockets</h1>"))
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
}
