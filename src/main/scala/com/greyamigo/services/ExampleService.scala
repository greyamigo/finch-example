package com.greyamigo.services

import java.io.File

import com.greyamigo.models.Message
import com.twitter.util.Future

import scala.io.Source


class ExampleService {
  val message = Message("Hello, world!")

  def getMessage(): Future[Message] = {
    Future.value(message)
  }

  def acceptMessage(incomingMessage: Message): Future[Message] = {
    Future.value(incomingMessage)
  }
  def acceptFile(incomingFile:File) : Future[Message] = {
    Future.value(Message(Source.fromFile(incomingFile).getLines().toString()))
  }

}
