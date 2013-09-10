package com.gleyzer.simpleproxy

import com.twitter.finagle.{Filter, Group, Http, Service, SimpleFilter}
import com.twitter.logging.Logger
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import com.twitter.util.{Stopwatch, SynchronizedLruMap}
import java.net.{InetSocketAddress, SocketAddress}
import org.jboss.netty.handler.codec.http._


object Main extends TwitterServer {
  val origin = flag("origin", new InetSocketAddress("xkcd.com", 80), "Origin Server")
  val listenOn = flag("http.main", new InetSocketAddress(8888), "Socket to listen on")

  val logFilter = new SimpleFilter[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest, service: Service[HttpRequest, HttpResponse]): Future[HttpResponse] = {
      val elapsed = Stopwatch.start()
      service(req) onSuccess { res =>
        log.info("%s %s => %d in %d ms",
          req.getMethod,
          req.getUri,
          res.getStatus.getCode,
          elapsed().inMillis)
      }
    }
  }

  val headerFilter = new SimpleFilter[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest, service: Service[HttpRequest, HttpResponse]): Future[HttpResponse] = {
      service(req) map { rsp =>
        rsp.setHeader("Server", "Finagle-Demo 1.0")
        rsp
      }
    }
  }

  def originService = Http.newService(Group[SocketAddress](origin()))

  def main() {

    val service = headerFilter andThen logFilter andThen originService

    val server = Http.serve(listenOn(), service)

    onExit {
      server.close()
    }

    Await.ready(server)
  }
}
