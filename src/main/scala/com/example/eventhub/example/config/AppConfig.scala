package com.example.eventhub.example.config

import pureconfig.*
import pureconfig.generic.auto.*

final case class Topics(telemetry: String)
final case class EventHub(connectionString: String, eventHubName: String, consumerGroup: String)

final case class AppConfig(topics: Topics, eventHub: EventHub)

object AppConfig {
  def load(): AppConfig = ConfigSource.default.load[AppConfig] match {
    case Right(appConfig) => appConfig
    case Left(e) =>
      throw new RuntimeException(s"Could not load configuration and therefore cannot run the application: $e")
  }
}
