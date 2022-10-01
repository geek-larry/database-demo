package com.jade.es.controller;

import com.jade.elasticsearchdb.dto.IndicesRecordDto;
import com.jade.elasticsearchdb.utils.ElasticUtil;
import com.jade.elasticsearchdb.utils.HttpStatus;
import com.jade.elasticsearchdb.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/index")
@Slf4j
public class IndexController {

    @Autowired
    private ElasticUtil elasticUtil;

    /**
     * 获取所有索引
     * @return
     */
    @GetMapping("/all")
    public Result<Object> allIndex(HttpServletRequest servletRequest) {
        List<Map<String, Object>> list = new ArrayList<>();
        String json = null;
        try {
            String endPoint = "/_cat/indices";
            String indexName = servletRequest.getParameter("indexName");
            if (!StringUtils.isEmpty(indexName)) {
                endPoint = endPoint + "/" + indexName + "*";
            }
            Request request = new Request("GET", endPoint);
            Response response = elasticUtil.getRestClient().performRequest(request);
            json = elasticUtil.toJson(response);
            String[] allindexes = json.split("\\n");
            for (String index : allindexes) {
                String[] idxContents = index.split("\\s+");
                if (idxContents.length >= 10) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("health", idxContents[0]);
                    map.put("status", idxContents[1]);
                    map.put("indexName", idxContents[2]);
                    map.put("uuid", idxContents[3]);
                    map.put("priNum", idxContents[4]);
                    map.put("repNum", idxContents[5]);
                    map.put("docCount", idxContents[6]);
                    map.put("docsDelCount", idxContents[7]);
                    map.put("storeSize", idxContents[8]);
                    map.put("priStoreSize", idxContents[9]);
                    list.add(map);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.builder().data(list).code(HttpStatus.OK.getCode()).build();
    }

    @GetMapping("/remove/{indexName}")
    public Result<Object> deleteIndex(@PathVariable("indexName") String indexName) {
        if (StringUtils.isEmpty(indexName)) {
            return Result.builder().code(HttpStatus.OK.getCode()).message(HttpStatus.INDEX_NOT_FOUND.getMessage()).build();
        }

        String json = null;
        try {
            Request request = new Request("DELETE", "/" + indexName);
            json = elasticUtil.toJson(elasticUtil.getRestClient().performRequest(request));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.builder().code(HttpStatus.OK.getCode()).message(json).build();
    }

    /**
     * 创建索引
     * @return
     */
    @PostMapping("/create")
    public Result<Object> createIndex(@RequestBody IndicesRecordDto dto) {
        Result.ResultBuilder<Object> builder = Result.builder();
        if (StringUtils.isEmpty(dto.getIndexName())) {
            return builder.code(HttpStatus.SERVER_FAIL.getCode()).message("索引名不能为空").build();
        }

        CreateIndexRequest indexRequest = new CreateIndexRequest(dto.getIndexName());
        indexRequest.settings(Settings.builder()
            .put("number_of_shards", dto.getPriNum()).put("number_of_replicas", dto.getRepliNum())
        );
        String message = "";
        try {
            CreateIndexResponse response = elasticUtil.getHighLevelClient().indices().create(indexRequest, RequestOptions.DEFAULT);
            if (response.isAcknowledged()) {
                message = "创建成功";
            } else {
                message = "创建失败";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.builder().code(HttpStatus.OK.getCode()).data(message).build();
    }

    /**
     * 索引详情
     * @return
     */
    @GetMapping("/detail/{indexName}")
    public Result<Object> indexDetail(@PathVariable("indexName") String indexName) {
        Result.ResultBuilder<Object> builder = Result.builder();
        if (StringUtils.isEmpty(indexName)) {
            return builder.code(HttpStatus.SERVER_FAIL.getCode()).message("索引名不能为空").build();
        }
        String json = null;
        try {
            Request request = new Request("GET", "/" + indexName);
            json = elasticUtil.toJson(elasticUtil.getRestClient().performRequest(request));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.builder().code(HttpStatus.OK.getCode()).data(json).build();
    }

}
