import Listener.MessageHandler
import Listener.echo
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.IncomingConnection
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString

object Listener {
  type MessageHandler = Int => Flow[String, String, NotUsed]
  val echo: Flow[String, String, NotUsed] = Flow[String].map(message => message)
}

class Listener(val messageHandler: MessageHandler = (_) => echo)(implicit val materializer: ActorMaterializer) {

  private val readFrame = Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)
  private val decode = Flow[ByteString].map(_.utf8String)
  private val encode = Flow[String].map(message => ByteString(message + "\n"))

  private def listenerFlow(id: Int) = Flow[ByteString]
    .via(readFrame)
    .via(decode)
    .via(messageHandler(id))
    .via(encode)

  def handle(connection: IncomingConnection): Unit = {
    println(s"New connection from: ${connection.remoteAddress}")

    val id = connection.remoteAddress.getPort
    connection.handleWith(listenerFlow(id))
  }
}
