import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object NotificationRoute {

  def apply(notificationService: NotificationService): Route =
    path("notify" / IntNumber) { id =>
      post {
        entity(as[String]) { message =>
          notificationService.publish(Notification(id, message))
          complete(HttpResponse(StatusCodes.Accepted))
        }
      }
    }
}
