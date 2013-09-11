package com.gleyzer.simpleproxy

import com.twitter.finagle.{Filter, Group, Http, Service, SimpleFilter}
import com.twitter.logging.Logger
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import com.twitter.util.{Stopwatch, SynchronizedLruMap}
import java.net.{InetSocketAddress, SocketAddress}
import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.stats.MetricsStatsReceiver


object Main extends TwitterServer {

  val originFlag = flag("origin", new InetSocketAddress("xkcd.com", 80), "Origin Server")

  val listenOnFlag = flag("http.main", new InetSocketAddress(8888), "Socket to listen on")

  val logFilter = new SimpleFilter[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest, service: Service[HttpRequest, HttpResponse]): Future[HttpResponse] = {
      val elapsed = Stopwatch.start()
      service(req) onSuccess { res =>
        log.info("%s %s => %d, %d bytes in %d ms",
          req.getMethod,
          req.getUri,
          res.getStatus.getCode,
          res.getContentLength,
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

  val contentSizeFilter = new SimpleFilter[HttpRequest, HttpResponse] {
    val stat = new MetricsStatsReceiver().stat("simpleproxy", "responseSize")

    def apply(req: HttpRequest, service: Service[HttpRequest, HttpResponse]): Future[HttpResponse] = {
      service(req) map { rsp =>
        stat.add(rsp.getContentLength)
        rsp
      }
    }
  }

  def originService = Http.newService(Group[SocketAddress](originFlag()))

  def main() {

    val service =
      headerFilter      andThen
      contentSizeFilter andThen
      logFilter         andThen
      originService

    val server = Http.serve(listenOnFlag(), service)

    onExit {
      server.close()
    }

    Await.ready(server)
  }
}
