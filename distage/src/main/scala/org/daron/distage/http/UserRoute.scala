package org.daron.distage.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import distage.{ModuleDef, TagK}
import org.daron.distage.ToFuture
import org.daron.distage.db.UserRepo
import org.daron.distage.domain.User

import scala.util.{Failure, Success}

class UserRoute[F[_]: ToFuture](repo: UserRepo[F]) {

  def route: Route = pathPrefix("users") {
    {
      (get & path(Segment)) { id =>
        onSuccess(ToFuture[F].toFuture(repo.find(id))) {
          case Some(u) => complete(u)
          case None    => complete(StatusCodes.NotFound)
        }
      }
    } ~ (post & entity(as[User])) { u =>
      onComplete(ToFuture[F].toFuture(repo.save(u))) {
        case Success(saved) => complete(saved)
        case Failure(_)     => complete(StatusCodes.BadRequest)
      }

    }
  }
}

object UserRoute {

  def UserRouteModule[F[_]: TagK: ToFuture] = new ModuleDef {
    many[Route].add { new UserRoute[F](_: UserRepo[F]).route }
    addImplicit[ToFuture[F]]
  }
}
