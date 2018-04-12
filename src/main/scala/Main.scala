import akka.actor.ActorSystem
import akka.kafka.{ProducerSettings, ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Producer
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Sink}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object Main extends App {
  
  implicit val system = ActorSystem("AkkaKafkaTest")
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()

  val servers = config.getString("test.kafka.servers")

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
  .withBootstrapServers(servers)

  val done = Source
    .tick(200 milliseconds, 30 seconds, "message")
    .map(_.toString)
    .map { elem =>
      println(s"Sink producer element: $elem")
      new ProducerRecord[Array[Byte], String]("akka-test1", elem)
    }
    .runWith(Producer.plainSink(producerSettings))

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(servers)
    .withGroupId("akka-stream-kafka-test")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  Consumer.committableSource(consumerSettings, Subscriptions.topics("akka-test1"))
    .map(msg => println(msg))
    .runWith(Sink.ignore)


}
