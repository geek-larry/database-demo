package com.jade.es.utils;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ElasticUtil {

    @Value("${elastic.host}")
    private String host;

    @Value("${elastic.port}")
    private Integer port;

    private volatile RestClient restClient;

    private volatile RestHighLevelClient highLevelClient;

    /**
     * 获取Rest连接
     * @return
     */
    public RestClient getRestClient() {
        if (restClient == null) {
            synchronized (ElasticUtil.class) {
                if (restClient == null) {
                    restClient = RestClient.builder(
                            new HttpHost(host, port, "http")).build();
                }
            }
        }
        return restClient;
    }

    public RestHighLevelClient getHighLevelClient() {
        if (highLevelClient == null) {
            synchronized (ElasticUtil.class) {
                if (highLevelClient == null) {
                    highLevelClient = new RestHighLevelClient(
                            RestClient.builder(
                                    new HttpHost(host, port, "http")));
                }
            }
        }
        return highLevelClient;
    }

    public String toJson(Response response) {
        String responseBody = null;
        try {
            responseBody = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseBody;
    }
}
