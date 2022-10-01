package com.jade.es.canal;

import cn.hutool.core.lang.ClassScanner;
import cn.hutool.core.util.StrUtil;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jade.elasticsearchdb.annotation.ESFieldAnnotation;
import com.jade.elasticsearchdb.utils.ElasticUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ElasticJob {

    private BlockingQueue<ESRowData> queue;

    private HashedWheelTimer wheelTimer;

    @Autowired
    private ElasticUtil elasticUtil;

    private static final Integer batchSize = 1000;

    private static final Map<String, Class> mapClass = new HashMap<>(16);

    @PostConstruct
    public void init() {
        queue = new ArrayBlockingQueue<ESRowData>(1000);
        wheelTimer = new HashedWheelTimer(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "es-write");
                return thread;
            }
        }, 1, TimeUnit.SECONDS, 100);

        log.info("时间轮任务启动");
        wheelTimer.start();

        wheelTimer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                transferData();
                wheelTimer.newTimeout(this, 1, TimeUnit.SECONDS);
            }
        }, 0, TimeUnit.SECONDS);

        log.info("扫描实体类");
        scanTable();
    }

    public void transferData(){
        List<ESRowData> dataList = new ArrayList<>();
        queue.drainTo(dataList, batchSize);
        BulkRequest bulkRequest = new BulkRequest();
        if (dataList.size() > 0) {
            log.info("开始写ES");
            dataList.forEach(item->{
                CanalEntry.EventType eventType = item.getEventType();
                if (eventType == CanalEntry.EventType.DELETE) {
                    DeleteRequest deleteRequest = new DeleteRequest(item.getTableName());
                    deleteRequest.id((String)item.getBeforeData().get("id"));
                    bulkRequest.add(deleteRequest);
                } else if (eventType == CanalEntry.EventType.INSERT) {
                    IndexRequest indexRequest = new IndexRequest(item.getTableName());
                    indexRequest.id((String) item.getAfterData().get("id"));
                    try {
                        indexRequest.source(sourceMap(item));
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    bulkRequest.add(indexRequest);
                } else if (eventType == CanalEntry.EventType.UPDATE) {
                    UpdateRequest updateRequest = new UpdateRequest(item.getTableName(), (String) item.getAfterData().get("id"));
                    try {
                        updateRequest.doc(sourceMap(item));
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    bulkRequest.add(updateRequest);
                }
            });
            if (bulkRequest.requests().size() > 0) {
                RestHighLevelClient highLevelClient = elasticUtil.getHighLevelClient();
                try {
                    BulkResponse bulkResponse = highLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    if (bulkResponse.hasFailures()) {
                        log.info(bulkResponse.buildFailureMessage());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log.info("写入结束");
        }
    }

    public void scanTable() {
        Set<Class<?>> classSet = ClassScanner.scanPackage("com.es.entity");
        Iterator<Class<?>> iterator = classSet.iterator();
        while (iterator.hasNext()) {
            Class<?> aClass = iterator.next();
            TableName tableName = aClass.getAnnotation(TableName.class);
            if (tableName != null) {
                mapClass.put(tableName.value(), aClass);
            }
        }
    }

    public Map<String, Object> sourceMap(ESRowData rowData) throws NoSuchFieldException {
        Class aClass = mapClass.get(rowData.getTableName());
        Map<String, Object> result = new HashMap<>();
        if (aClass != null) {
            Map<String, Object> data = rowData.getAfterData();
            Set<String> keySet = data.keySet();
            Iterator<String> iterator = keySet.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String fieldName = StrUtil.toCamelCase(key);
                Field field = aClass.getDeclaredField(fieldName);
                if (field != null) {
                    ESFieldAnnotation annotation = field.getAnnotation(ESFieldAnnotation.class);
                    String esKey = annotation.value();
                    String esType = annotation.type();
                    Object value = data.get(key);
                    if ("boolean".equals(esType)) {
                        result.put(esKey, Boolean.valueOf((String) value));
                    } else if ("double".equals(esType)){
                        result.put(esKey, Double.valueOf((String) value));
                    } else if ("integer".equals(esType)){
                        result.put(esKey, Integer.valueOf((String) value));
                    } else if ("geo_point".equals(esType)){
                        String[] location = esKey.split("\\.");
                        Map<String, Object> submap = new HashMap<>();
                        if (result.containsKey(location[0])) {
                            submap = (Map<String, Object>)result.get(location[0]);
                            submap.put(location[1], Double.parseDouble((String) value));
                        } else {
                            submap.put(location[1], Double.parseDouble((String) value));
                            result.put(location[0], submap);
                        }
                    } else if ("text".equals(esType)) {
                        result.put(esKey, value);
                    } else if ("keyword".equals(esType)) {
                        String valueStr = (String) value;
                        String[] tags = valueStr.split("[,|/]");
                        result.put(esKey, tags);
                    }
                }
            }
        }
        return result;
    }

    public BlockingQueue<ESRowData> getQueue() {
        return queue;
    }
}
