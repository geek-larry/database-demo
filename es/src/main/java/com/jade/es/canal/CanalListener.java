package com.jade.es.canal;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.*;
import com.alibaba.otter.canal.protocol.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CanalListener {

    @Value("${canal.host}")
    private String host;

    @Value("${canal.port}")
    private Integer port;

    @Value("${canal.destination}")
    private String destination;

    @Autowired
    private ElasticJob elasticJob;

    @PostConstruct
    public void init() {
        new Thread(()->{
            log.info("canal start connect {}:{}", host, port);
            CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(host, port), destination, "", "");
            log.info("canal connect successful {}:{}", host, port);

            int batchSize = 1000;
            try {
                connector.connect();
                connector.subscribe(".*\\..*");
                connector.rollback();
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                    long batchId = message.getId();
                    int size = message.getEntries().size();
                    if (batchId == -1 || size == 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        printEntry(message.getEntries());
                    }

                    connector.ack(batchId); // 提交确认
                    // connector.rollback(batchId); // 处理失败, 回滚数据
                }

                log.info("empty too many times, exit");
            } finally {
                connector.disconnect();
            }
        }).start();
    }

    private void printEntry(List<Entry> entrys) {
        for (Entry entry : entrys) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            RowChange rowChage = null;
            try {
                rowChage = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                        e);
            }

            EventType eventType = rowChage.getEventType();
            log.info(String.format("================&gt; binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                    eventType));

            for (RowData rowData : rowChage.getRowDatasList()) {
                ESRowData esRowData = new ESRowData();
                esRowData.setTableName(entry.getHeader().getTableName());
                if (eventType == EventType.DELETE) {
                    esRowData.setEventType(EventType.DELETE);
                    esRowData.setBeforeData(colToMap(rowData.getBeforeColumnsList()));
                } else if (eventType == EventType.INSERT) {
                    esRowData.setEventType(EventType.INSERT);
                    esRowData.setAfterData(colToMap(rowData.getAfterColumnsList()));
                } else {
                    log.info("-------&gt; before");
                    esRowData.setEventType(EventType.UPDATE);
                    esRowData.setBeforeData(colToMap(rowData.getBeforeColumnsList()));
                    log.info("-------&gt; after");
                    esRowData.setAfterData(colToMap(rowData.getAfterColumnsList()));
                }

                log.info(JSON.toJSONString(esRowData));
                elasticJob.getQueue().add(esRowData);
            }
        }
    }

    private Map colToMap(List<Column> columns) {
        Map<String, Object> map = new HashMap<>();
        for (Column column : columns) {
            map.put(column.getName(), column.getValue());
        }
        return map;
    }
}
