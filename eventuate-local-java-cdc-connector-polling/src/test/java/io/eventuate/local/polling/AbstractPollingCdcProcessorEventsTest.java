package io.eventuate.local.polling;

import io.eventuate.common.eventuate.local.PublishedEvent;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.local.common.BinlogEntryToPublishedEventConverter;
import io.eventuate.local.common.CdcDataPublisher;
import io.eventuate.local.test.util.SourceTableNameSupplier;
import io.eventuate.local.common.exception.EventuateLocalPublishingException;
import io.eventuate.local.test.util.CdcProcessorEventsTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Consumer;

public abstract class AbstractPollingCdcProcessorEventsTest extends CdcProcessorEventsTest {

  @Autowired
  protected PollingDao pollingDao;

  @Autowired
  private PollingDataProvider pollingDataProvider;

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  @Autowired
  private EventuateSchema eventuateSchema;

  @Autowired
  private SourceTableNameSupplier sourceTableNameSupplier;

  @Override
  protected void prepareBinlogEntryHandler(Consumer<PublishedEvent> consumer) {
    pollingDao.addBinlogEntryHandler(eventuateSchema,
            sourceTableNameSupplier.getSourceTableName(),
            new BinlogEntryToPublishedEventConverter(),
            new CdcDataPublisher<PublishedEvent>(null, null, null, null) {
              @Override
              public void handleEvent(PublishedEvent publishedEvent) throws EventuateLocalPublishingException {
                consumer.accept(publishedEvent);
              }
            });
  }

  @Override
  protected void startEventProcessing() {
    testHelper.runInSeparateThread(pollingDao::start);
  }

  @Override
  protected void stopEventProcessing() {
    pollingDao.stop();
  }
}
