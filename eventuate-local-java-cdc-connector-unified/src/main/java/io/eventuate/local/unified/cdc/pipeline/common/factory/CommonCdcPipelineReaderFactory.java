package io.eventuate.local.unified.cdc.pipeline.common.factory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.alibaba.druid.pool.DruidDataSource;
import io.eventuate.coordination.leadership.LeaderSelectorFactory;
import io.eventuate.local.common.BinlogEntryReader;
import io.eventuate.local.common.ConnectionPoolConfigurationProperties;
import io.eventuate.local.unified.cdc.pipeline.common.BinlogEntryReaderProvider;
import io.eventuate.local.unified.cdc.pipeline.common.properties.CdcPipelineReaderProperties;
import io.eventuate.local.unified.cdc.pipeline.common.properties.PoolConfigProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.Properties;

abstract public class CommonCdcPipelineReaderFactory<PROPERTIES extends CdcPipelineReaderProperties, READER extends BinlogEntryReader>
        implements CdcPipelineReaderFactory<PROPERTIES, READER> {

  protected MeterRegistry meterRegistry;
  protected LeaderSelectorFactory leaderSelectorFactory;
  protected BinlogEntryReaderProvider binlogEntryReaderProvider;
  protected ConnectionPoolConfigurationProperties connectionPoolConfigurationProperties;


  @Autowired
  PoolConfigProperties poolConfigProperties;

  public CommonCdcPipelineReaderFactory(MeterRegistry meterRegistry,
                                        LeaderSelectorFactory leaderSelectorFactory,
                                        BinlogEntryReaderProvider binlogEntryReaderProvider,
                                        ConnectionPoolConfigurationProperties connectionPoolConfigurationProperties) {
    this.meterRegistry = meterRegistry;
    this.leaderSelectorFactory = leaderSelectorFactory;
    this.binlogEntryReaderProvider = binlogEntryReaderProvider;
    this.connectionPoolConfigurationProperties = connectionPoolConfigurationProperties;
  }

  public abstract READER create(PROPERTIES cdcPipelineReaderProperties);

  protected DataSource createDataSource(PROPERTIES properties) {

    DataSource dataSource=null;
if(poolConfigProperties.getName()!=null && "hikari".equals(poolConfigProperties.getName())) {
  HikariDataSource hikariDataSource = new HikariDataSource();
  hikariDataSource.setUsername(properties.getDataSourceUserName());
  hikariDataSource.setPassword(properties.getDataSourcePassword());
  hikariDataSource.setJdbcUrl(properties.getDataSourceUrl());
  hikariDataSource.setDriverClassName(properties.getDataSourceDriverClassName());
  hikariDataSource.setConnectionTestQuery("select 1");
}
    if (poolConfigProperties.getName()!=null && "druid".equals(poolConfigProperties.getName())) {
      DruidDataSource druidDataSource = new DruidDataSource();

      druidDataSource.setUsername(properties.getDataSourceUserName());
      druidDataSource.setPassword(properties.getDataSourcePassword());
      druidDataSource.setUrl(properties.getDataSourceUrl());
      druidDataSource.setDriverClassName(properties.getDataSourceDriverClassName());

      druidDataSource.setInitialSize(poolConfigProperties.getInitialSize());
      druidDataSource.setMaxActive(poolConfigProperties.getMaxActive());
      druidDataSource.setMinIdle(poolConfigProperties.getMinIdle());
      druidDataSource.setMaxWait(poolConfigProperties.getMaxWait());
      druidDataSource.setTimeBetweenEvictionRunsMillis(poolConfigProperties.getTimeBetweenEvictionRunsMillis());
      druidDataSource.setMinEvictableIdleTimeMillis(poolConfigProperties.getMinEvictableIdleTimeMillis());
      druidDataSource.setValidationQuery(poolConfigProperties.getValidationQuery());
      druidDataSource.setTestWhileIdle(poolConfigProperties.getTestWhileIdle());
      druidDataSource.setTestOnBorrow(poolConfigProperties.getTestOnBorrow());
      druidDataSource.setTestOnReturn(poolConfigProperties.getTestOnReturn());
      druidDataSource.setPoolPreparedStatements(poolConfigProperties.getPoolPreparedStatements());

      try {
        druidDataSource.setFilters(poolConfigProperties.getFilters());
      } catch (Exception e) {
      }

      druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(poolConfigProperties.getMaxPoolPreparedStatementPerConnectionSize());
//    Properties properties1=new Properties();
//    properties1.set
//    druidDataSource.setConnectProperties(druidConfigProperties.getConnectionProperties());
//
      druidDataSource.setUseGlobalDataSourceStat(poolConfigProperties.getUseGlobalDataSourceStat());


      //String t=druidConfigProperties.getType();


      dataSource = druidDataSource;
    }

    return dataSource;
  }
}
