package com.jade.es.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class ESRowData implements Serializable {
    private Map<String, Object> beforeData = new HashMap<>();

    private Map<String, Object> afterData = new HashMap<>();

    private CanalEntry.EventType eventType;

    private String tableName;
}
