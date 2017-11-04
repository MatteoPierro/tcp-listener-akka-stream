import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("Listener")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val notificationService = new NotificationService(system)

  val messageHandler = new EchoWithNotification(notificationService)
  val listener: Listener = new Listener(messageHandler)

  val connections = Tcp().bind("127.0.0.1", 9999)
  connections.runForeach(listener.handle)

  Http().bindAndHandle(NotificationRoute(notificationService), "localhost", 8080)
}
