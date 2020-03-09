package io.eventuate.local.polling;

import com.google.common.collect.ImmutableMap;
import com.sun.rowset.JdbcRowSetImpl;
import io.eventuate.common.eventuate.local.BinLogEvent;
import io.eventuate.common.eventuate.local.BinlogFileOffset;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.common.jdbc.sqldialect.EventuateSqlDialect;
import io.eventuate.local.common.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import javax.sql.rowset.JdbcRowSet;
import com.sun.rowset.JdbcRowSetImpl;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class PollingDao extends BinlogEntryReader {
  private static final String PUBLISHED_FIELD = "published";

  private DataSource dataSource;
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private int maxEventsPerPolling;
  private int maxAttemptsForPolling;
  private int pollingRetryIntervalInMilliseconds;
  private int pollingIntervalInMilliseconds;
  private Map<SchemaAndTable, String> pkFields = new HashMap<>();
  private EventuateSqlDialect eventuateSqlDialect;

  private PollingProcessingStatusService pollingProcessingStatusService;

  public PollingDao(MeterRegistry meterRegistry,
                    String dataSourceUrl,
                    DataSource dataSource,
                    int maxEventsPerPolling,
                    int maxAttemptsForPolling,
                    int pollingRetryIntervalInMilliseconds,
                    int pollingIntervalInMilliseconds,
                    String readerName,
                    EventuateSqlDialect eventuateSqlDialect) {

    super(meterRegistry,
            dataSourceUrl,
            dataSource,
            readerName);

    if (maxEventsPerPolling <= 0) {
      throw new IllegalArgumentException("Max events per polling parameter should be greater than 0.");
    }

    this.dataSource = dataSource;
    this.pollingIntervalInMilliseconds = pollingIntervalInMilliseconds;
    this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    this.maxEventsPerPolling = maxEventsPerPolling;
    this.maxAttemptsForPolling = maxAttemptsForPolling;
    this.pollingRetryIntervalInMilliseconds = pollingRetryIntervalInMilliseconds;
    this.eventuateSqlDialect = eventuateSqlDialect;

    pollingProcessingStatusService = new PollingProcessingStatusService(dataSource, PUBLISHED_FIELD, eventuateSqlDialect);
  }

  @Override
  public CdcProcessingStatusService getCdcProcessingStatusService() {
    return pollingProcessingStatusService;
  }

  @Override
  public <EVENT extends BinLogEvent> BinlogEntryHandler addBinlogEntryHandler(EventuateSchema eventuateSchema, String sourceTableName, BinlogEntryToEventConverter<EVENT> binlogEntryToEventConverter, CdcDataPublisher<EVENT> dataPublisher) {
    BinlogEntryHandler binlogEntryHandler = super.addBinlogEntryHandler(eventuateSchema, sourceTableName, binlogEntryToEventConverter, dataPublisher);
    pollingProcessingStatusService.addTable(binlogEntryHandler.getQualifiedTable());
    return binlogEntryHandler;
  }

  @Override
  public void start() {
    super.start();

    stopCountDownLatch = new CountDownLatch(1);
    running.set(true);

    while (running.get()) {
      int processedEvents = 0;
      try {
        processedEvents = binlogEntryHandlers.stream().map(this::processEvents).reduce(0, (a, b) -> a + b);
      } catch (Exception e) {
        handleProcessingFailException(e);
      }

      try {
        if (processedEvents == 0) {
          Thread.sleep(pollingIntervalInMilliseconds);
        }
      } catch (InterruptedException e) {
        handleProcessingFailException(e);
      }
    }

    stopCountDownLatch.countDown();
  }

  public int processEvents(BinlogEntryHandler handler) {

    String pk = getPrimaryKey(handler);

    String findEventsQuery = eventuateSqlDialect.addLimitToSql(String.format("SELECT * FROM %s WHERE %s = 0 ORDER BY %s ASC",
            handler.getQualifiedTable(), PUBLISHED_FIELD, pk), ":limit");

    SqlRowSet sqlRowSet = DaoUtils.handleConnectionLost(maxAttemptsForPolling,
            pollingRetryIntervalInMilliseconds,
            () -> namedParameterJdbcTemplate.queryForRowSet(findEventsQuery, ImmutableMap.of("limit", maxEventsPerPolling)),
            this::onInterrupted,
            running);

    List<Object> ids = new ArrayList<>();

    while (sqlRowSet.next()) {
      ids.add(sqlRowSet.getObject(pk));

      handler.publish(new BinlogEntry() {
        @Override
        public Object getColumn(String name) {
          return sqlRowSet.getObject(name);
        }

        @Override
        public BinlogFileOffset getBinlogFileOffset() {
          return null;
        }
      });

      onEventReceived();
    }

    if (ids.isEmpty())
      onActivity();
    else {

      String markEventsAsReadQuery = String.format("UPDATE %s SET %s = 1 WHERE %s in (:ids)",
              handler.getQualifiedTable(), PUBLISHED_FIELD, pk);

      DaoUtils.handleConnectionLost(maxAttemptsForPolling,
              pollingRetryIntervalInMilliseconds,
              () -> namedParameterJdbcTemplate.update(markEventsAsReadQuery, ImmutableMap.of("ids", ids)),
              this::onInterrupted,
              running);
    }

    return ids.size();
  }

  private String getPrimaryKey(BinlogEntryHandler handler) {
    SchemaAndTable schemaAndTable = handler.getSchemaAndTable();

    if (pkFields.containsKey(schemaAndTable)) {
      return pkFields.get(schemaAndTable);
    }

    String pk = DaoUtils.handleConnectionLost(maxAttemptsForPolling,
            pollingRetryIntervalInMilliseconds,
            () -> queryPrimaryKey(handler),
            this::onInterrupted,
            running);

    pkFields.put(schemaAndTable, pk);

    return pk;
  }

  private String queryPrimaryKey(BinlogEntryHandler handler) throws SQLException {
    String pk;
//    Connection connection = null;
//    try {
//      String dbName=handler.getSchemaAndTable().getSchema();
//      connection = dataSource.getConnection();
//      JdbcRowSet jrs=new JdbcRowSetImpl(connection);
//      jrs.setCommand("use "+dbName);
//      jrs.execute();
//      String sql=String.format("SELECT column_name FROM INFORMATION_SCHEMA.`KEY_COLUMN_USAGE` WHERE table_name='%s' AND constraint_name='PRIMARY'","message");
//      jrs.setCommand(sql);
//      jrs.execute();
//
//
//      ResultSet resultSet = connection
//              .getMetaData()
//              .getPrimaryKeys(null,
//                      handler.getSchemaAndTable().getSchema(),
//                      handler.getSchemaAndTable().getTableName());
//
//      if (resultSet.next()) {
//        pk = resultSet.getString("COLUMN_NAME");
//        if (resultSet.next()) {
//          throw new RuntimeException("Table %s has more than one primary key");
//        }
//      } else {
//        throw new RuntimeException("Cannot get table: result set is empty");
//      }
//    } finally {
//      try {
//        if (connection != null) {
//          connection.close();
//        }
//      } catch (SQLException e) {
//        logger.warn(e.getMessage(), e);
//      }
//    }

    pk="id";
    return pk;
  }

  private void onInterrupted() {
    running.set(false);
    stopCountDownLatch.countDown();
  }
}
