package com.jade.es.controller;

import com.jade.elasticsearchdb.dto.DocQueryDto;
import com.jade.elasticsearchdb.utils.ElasticUtil;
import com.jade.elasticsearchdb.utils.HttpStatus;
import com.jade.elasticsearchdb.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/docquery")
public class DocQueryController {

    @Autowired
    private ElasticUtil elasticUtil;

//    @PostMapping("/{indexName}/search")
//    public Result<Object> search(@RequestBody DocQueryDto queryDto, @PathVariable("indexName") String indexName) {
//        RestHighLevelClient client = elasticUtil.getHighLevelClient();
//        SearchRequest searchRequest = new SearchRequest(indexName);
//        SearchSourceBuilder builder = new SearchSourceBuilder();
//        if (queryDto.getFrom() != null && queryDto.getFrom() > 0) {
//            builder.from(queryDto.getFrom());
//        }
//        if (queryDto.getSize() != null && queryDto.getSize() > 0) {
//            builder.size(queryDto.getSize());
//        }
//        if (!queryDto.getQuery().isEmpty()) {
//            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
//            QueryBuilder queryBuilder = explainParam(queryDto);
//            String boolType = (String) queryDto.getQuery().get("boolType");
//            if (boolType.equals("must")) {
//                boolQueryBuilder.must().add(queryBuilder);
//            } else if (boolType.equals("must_not")) {
//                boolQueryBuilder.mustNot().add(queryBuilder);
//            } else if (boolType.equals("should")) {
//                boolQueryBuilder.should().add(queryBuilder);
//            }
//            builder.query(boolQueryBuilder);
//        }
//        searchRequest.source(builder);
//        StringBuilder dest = new StringBuilder("");
//        Map<String, Object> map = new HashMap<>();
//        try {
//            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
//            TotalHits totalHits = response.getHits().getTotalHits();
//            map.put("total", totalHits.value);
//            SearchHits searchHits = response.getHits();
//            SearchHit[] searchHitsHits = searchHits.getHits();
//            List<Object> dataList = new ArrayList<>();
//            List<String> colList = new ArrayList<>();
//            for (SearchHit hit : searchHitsHits) {
//                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
//                sourceAsMap.put("docId", hit.getId());
//                sourceAsMap.put("indexName", hit.getIndex());
//                dataList.add(sourceAsMap);
//                dsfMap(colList, sourceAsMap, null);
//            }
//            map.put("tableCols", colList);
//            map.put("data", dataList);
//
//            // 获取mappings
//            GetIndexRequest indexRequest = new GetIndexRequest(indexName);
//            GetIndexResponse indexResponse = client.indices().get(indexRequest, RequestOptions.DEFAULT);
//            org.elasticsearch.cluster.metadata.MappingMetaData indexMappings = indexResponse.getMappings().get(indexName);
//            Map<String, Object> indexTypeMappings = indexMappings.getSourceAsMap();
//            map.put("tableColsType", indexTypeMappings.get("properties"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return Result.builder().data(map).code(HttpStatus.OK.getCode()).build();
//    }

    /**
     * 删除文档
     * @param indexName
     * @param docId
     * @return
     */
    @GetMapping("/{indexName}/delete/{docId}")
    public Result<Object> search(@PathVariable("indexName") String indexName, @PathVariable("docId") String docId) {
        DeleteRequest deleteRequest = new DeleteRequest(indexName, docId);
        RestHighLevelClient client = elasticUtil.getHighLevelClient();
        try {
            DeleteResponse deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.builder().data(null).code(HttpStatus.OK.getCode()).build();
    }

    public void dsfMap(List<String> colList, Map<String, Object> sourceAsMap, String parentKey) {
        sourceAsMap.forEach((k, v) -> {
            String key = StringUtils.isEmpty(parentKey) ? k : parentKey + "." + k;
            if (v instanceof Map) {
                dsfMap(colList, (Map<String, Object>) v, key);
            } else if (v instanceof List) {
                List list = (List) v;
                boolean isStr = false;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        dsfMap(colList, (Map<String, Object>) item, key);
                    } else {
                        isStr = true;
                        break;
                    }
                }
                if (isStr) {
                    if (!colList.contains(key)) {
                        colList.add(key);
                    }
                }
            } else {
                if (!colList.contains(key)) {
                    colList.add(key);
                }
            }
        });
    }

    public QueryBuilder explainParam(DocQueryDto queryDto) {
        String fieldValue = (String) queryDto.getQuery().get("fieldValue");
        String opType = (String) queryDto.getQuery().get("opType");
        String qual = (String) queryDto.getQuery().get("qual");
        String rangeGtType = (String) queryDto.getQuery().get("rangeGtType");
        String rangeStart = (String) queryDto.getQuery().get("rangeStart");
        String rangeLtType = (String) queryDto.getQuery().get("rangeLtType");
        String rangeEnd = (String) queryDto.getQuery().get("rangeEnd");
        String fuzzyValue1 = (String) queryDto.getQuery().get("fuzzyValue1");
        String fuzzyType = (String) queryDto.getQuery().get("fuzzyType");
        String fuzzyValue2 = (String) queryDto.getQuery().get("fuzzyValue2");

        QueryBuilder queryBuilder = null;
        if (fieldValue.equals("match_all") || StringUtils.isEmpty(qual)) {
            return new MatchAllQueryBuilder();
        }
        if (fieldValue.equals("_all")) {
            return new QueryStringQueryBuilder(qual);
        } else {
            // 既不是match_all也不是_all，就是普通字段
            if (opType.equals("term")) {
                queryBuilder = new TermQueryBuilder(fieldValue, qual);
            } else if (opType.equals("range")) {
                RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(fieldValue);
                if ("gt".equals(rangeGtType)) {
                    rangeQueryBuilder.gt(rangeStart);
                } else if ("gte".equals(rangeGtType)) {
                    rangeQueryBuilder.gte(rangeStart);
                }
                if ("lt".equals(rangeLtType)) {
                    rangeQueryBuilder.lt(rangeEnd);
                } else if ("lte".equals(rangeLtType)) {
                    rangeQueryBuilder.lte(rangeEnd);
                }
                queryBuilder = rangeQueryBuilder;
            } else if (opType.equals("query_string")) {
                queryBuilder = new QueryStringQueryBuilder(qual);

            } else if (opType.equals("fuzzy")) {
                FuzzyQueryBuilder fuzzyQueryBuilder = new FuzzyQueryBuilder(fieldValue, fuzzyValue1);
                if ("max_expansions".equals(fuzzyType)) {
                    fuzzyQueryBuilder.maxExpansions(Integer.parseInt(fuzzyValue2));
                } else if ("min_similarity".equals(fuzzyType)) {
                }
            } else if (opType.equals("missing")) {
            } else if (opType.equals("text")) {
                queryBuilder = new MatchQueryBuilder(fieldValue, qual);
            } else if (opType.equals("prefix")) {
                queryBuilder = new PrefixQueryBuilder(fieldValue, qual);
            } else if (opType.equals("wildcard")) {
                queryBuilder = new WildcardQueryBuilder(fieldValue, qual);
            }
        }
        return queryBuilder;
    }
}
