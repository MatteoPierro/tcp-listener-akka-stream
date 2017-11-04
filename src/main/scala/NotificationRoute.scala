import NotificationService.Notify
import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object NotificationRoute {
  def apply(notificationService: ActorRef): Route =
    path("notify" / IntNumber) { id =>
      post {
        entity(as[String]) { message =>
          notificationService ! Notify(Notification(id, message))
          complete(HttpResponse(StatusCodes.Accepted))
        }
      }
    }
}
