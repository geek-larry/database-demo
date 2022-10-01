package com.jade.es.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DocQueryDto implements Serializable {
    private Integer from;
    private Integer size;
    private Map<String, Object> aggs;
    private Map<String, Object> query;
}
