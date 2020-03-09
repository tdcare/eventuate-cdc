package io.eventuate.tram.connector;

import com.google.common.collect.ImmutableSet;
import io.eventuate.common.spring.jdbc.sqldialect.SqlDialectConfiguration;
import io.eventuate.messaging.kafka.basic.consumer.EventuateKafkaConsumerConfigurationProperties;
import io.eventuate.messaging.kafka.common.EventuateKafkaConfigurationProperties;
import io.eventuate.messaging.kafka.spring.common.EventuateKafkaPropertiesConfiguration;
import io.eventuate.messaging.kafka.consumer.MessageConsumerKafkaImpl;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.function.Consumer;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {EventuateTramCdcKafkaTest.Config.class})
public class EventuateTramCdcKafkaTest extends AbstractTramCdcTest {

  @Configuration
  @EnableAutoConfiguration
  @Import({EventuateKafkaPropertiesConfiguration.class,
          SqlDialectConfiguration.class})
  public static class Config {
  }

  @Autowired
  private EventuateKafkaConfigurationProperties eventuateKafkaConfigurationProperties;

  @Override
  protected void createConsumer(String topic, Consumer<String> consumer) {
    MessageConsumerKafkaImpl messageConsumerKafka = new MessageConsumerKafkaImpl(eventuateKafkaConfigurationProperties.getBootstrapServers(),
            EventuateKafkaConsumerConfigurationProperties.empty());

    messageConsumerKafka.subscribe(subscriberId,
            ImmutableSet.of(topic),
            kafkaMessage -> consumer.accept(kafkaMessage.getPayload()));
  }
}
