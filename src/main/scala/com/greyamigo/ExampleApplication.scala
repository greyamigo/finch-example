package com.greyamigo

import java.io.File

import com.greyamigo.models.Message
import com.greyamigo.services.ExampleService
import com.twitter.app.Flag
import com.twitter.finagle.Http
import com.twitter.finagle.http.exp.Multipart.{FileUpload, InMemoryFileUpload}
import com.twitter.io.{Buf, Bufs, Reader}
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch.circe._
import io.finch.{Application, Endpoint, _}
import shapeless.Witness


object ExampleApplication extends TwitterServer {
  val port: Flag[Int] = flag("port", 9000, "TCP port for HTTP server")

  val exampleService = new ExampleService

  def hello: Endpoint[Message] = get("hello") {
    exampleService.getMessage().map(Ok)
  }

  def acceptedMessage: Endpoint[Message] = jsonBody[Message]


  def accept: Endpoint[Message] = post("accept" :: acceptedMessage) { incomingMessage: Message =>
    exampleService.acceptMessage(incomingMessage).map(Ok)
  }

  def acceptFile: Endpoint[Message] = post("acceptfile" :: fileUpload("file")) {
    upload: FileUpload => {
      exampleService.acceptMessage(Message(upload.fileName ++ "::" ++ upload.contentTransferEncoding ++ "::" ++ upload.contentType)).map(Ok)
    }
  }

  def acceptFileMemmory: Endpoint[Message] = post("acceptfilem" :: fileUpload("file")) {
    x: FileUpload => {
      exampleService.acceptMessage(Message(Bufs.asUtf8String(x.asInstanceOf[InMemoryFileUpload].content))).map(Ok)
    }
  }

  def getAFileBack: Endpoint[Buf] = get("getafileback" :: param("path")) {
    path: String =>
      val reader: Reader = Reader.fromFile(new File(path))
      Reader.readAll(reader).map(Ok)
  }


  val headersAll = root.map(_.headerMap.toMap)

  def headers = get("helloheaders" :: headersAll) { headers: Map[String, String] =>
    Ok(s"Headers: $headers")
  }

  val api = (hello :+: accept :+: acceptFile :+: acceptFileMemmory :+: headers).handle {
    case e: Exception => {
      e.printStackTrace(); InternalServerError(e)
    }
  }

  val fileapi = getAFileBack.handle({
    case e: Exception => {
      e.printStackTrace(); InternalServerError(e)
    }

  })
  type Zip = Witness.`"application/zip"`.T


  def main(): Unit = {
    log.info(s"Serving the application on port ${port()}")


    val service = Bootstrap
      .serve[Application.Json](api)
      .serve[Zip](fileapi)
      .toService

    val server =
      Http.server
        .withStatsReceiver(statsReceiver)
        .serve(s":${port()}", service)

    Await.ready(adminHttpServer)
  }
}
