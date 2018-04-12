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

object Main extends App {
  
  implicit val system = ActorSystem("AkkaKafkaTest")
  implicit val materializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
  .withBootstrapServers("localhost:9092")

  val done = Source(1 to 10)
    .map(_.toString)
    .map { elem =>
      println(s"Sink producer element: $elem")
      new ProducerRecord[Array[Byte], String]("akka_test1", elem)
    }
    .runWith(Producer.plainSink(producerSettings))

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("akka-stream-kafka-test")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  Consumer.committableSource(consumerSettings, Subscriptions.topics("akka_test1"))
    .map(msg => println(msg))
    .runWith(Sink.ignore)


}
