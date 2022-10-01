package com.jade.es.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TableData<T> implements Serializable {
    private Integer from;

    private Integer size;

    private Long total;

    private List<T> data;

    private T query;
}
