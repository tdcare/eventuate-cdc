package io.eventuate.cdc.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        RedisAutoConfiguration.class})
//@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
//        DataSourceTransactionManagerAutoConfiguration.class,
//        RedisAutoConfiguration.class})
@Import({DruidConfig.class})
public class EventuateCdcServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(EventuateCdcServiceMain.class, args);
  }
}
