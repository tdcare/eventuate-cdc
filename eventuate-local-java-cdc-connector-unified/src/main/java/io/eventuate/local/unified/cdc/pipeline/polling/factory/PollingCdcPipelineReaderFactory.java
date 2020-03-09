package io.eventuate.local.unified.cdc.pipeline.polling.factory;

import io.eventuate.common.jdbc.sqldialect.SqlDialectSelector;
import io.eventuate.coordination.leadership.LeaderSelectorFactory;
import io.eventuate.local.common.ConnectionPoolConfigurationProperties;
import io.eventuate.local.polling.PollingDao;
import io.eventuate.local.unified.cdc.pipeline.common.BinlogEntryReaderProvider;
import io.eventuate.local.unified.cdc.pipeline.common.factory.CommonCdcPipelineReaderFactory;
import io.eventuate.local.unified.cdc.pipeline.polling.properties.PollingPipelineReaderProperties;
import io.micrometer.core.instrument.MeterRegistry;

public class PollingCdcPipelineReaderFactory extends CommonCdcPipelineReaderFactory<PollingPipelineReaderProperties, PollingDao> {

  public static final String TYPE = "polling";

  private SqlDialectSelector sqlDialectSelector;

  public PollingCdcPipelineReaderFactory(MeterRegistry meterRegistry,
                                         LeaderSelectorFactory leaderSelectorFactory,
                                         BinlogEntryReaderProvider binlogEntryReaderProvider,
                                         SqlDialectSelector sqlDialectSelector,
                                         ConnectionPoolConfigurationProperties connectionPoolConfigurationProperties) {

    super(meterRegistry, leaderSelectorFactory, binlogEntryReaderProvider, connectionPoolConfigurationProperties);

    this.sqlDialectSelector = sqlDialectSelector;
  }

  @Override
  public boolean supports(String type) {
    return TYPE.equals(type);
  }

  @Override
  public PollingDao create(PollingPipelineReaderProperties readerProperties) {

    return new PollingDao(meterRegistry,
            readerProperties.getDataSourceUrl(),
            createDataSource(readerProperties),
            readerProperties.getMaxEventsPerPolling(),
            readerProperties.getMaxAttemptsForPolling(),
            readerProperties.getPollingRetryIntervalInMilliseconds(),
            readerProperties.getPollingIntervalInMilliseconds(),
            readerProperties.getReaderName(),
            sqlDialectSelector.getDialect(readerProperties.getDataSourceDriverClassName()));
  }

  @Override
  public Class<PollingPipelineReaderProperties> propertyClass() {
    return PollingPipelineReaderProperties.class;
  }
}
