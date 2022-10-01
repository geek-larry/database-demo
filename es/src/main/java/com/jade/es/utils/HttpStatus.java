package com.jade.es.utils;

public enum HttpStatus {

    SERVER_FAIL(500, "服务端错误"),
    INDEX_NOT_FOUND(20004, "索引不存在"),
    OK(20000,"返回正常");
    private int code;
    private String message;

    HttpStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
