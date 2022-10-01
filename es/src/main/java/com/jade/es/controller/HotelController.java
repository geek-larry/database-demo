package com.jade.es.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jade.elasticsearchdb.dto.HotelData;
import com.jade.elasticsearchdb.dto.HotelDto;
import com.jade.elasticsearchdb.entity.Hotel;
import com.jade.elasticsearchdb.service.IHotelService;
import com.jade.elasticsearchdb.utils.ElasticUtil;
import com.jade.elasticsearchdb.utils.HttpStatus;
import com.jade.elasticsearchdb.utils.KafkaUtil;
import com.jade.elasticsearchdb.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
@Slf4j
public class HotelController {
    @Autowired
    private IHotelService hotelService;

    @Autowired
    private ElasticUtil elasticUtil;

    @Autowired
    private KafkaUtil kafkaUtil;

    @Value("${hotel.topic}")
    private String topicName;

    private static final String INDEX_NAME = "t_hotel";

    private static final String SUGGEST_INDEX_NAME = "t_hotel_suggest";

    private static final String chineseSugName="hotel_chinese_sug";
    private static final String fullPinyinSugName="hotel_full_pinyin_sug";
    private static final String headPinyinSugName="hotel_head_pinyin_sug";

    @PostMapping("/addHotel")
    public Result<Object> addHotel(@RequestBody HotelDto hotelDto) {
        String message = null;
        Hotel one = hotelService.getOne(new QueryWrapper<Hotel>().eq("id", hotelDto.getId()));
        if (one != null) {
            message = "主键已经存在，请重新添加";
        } else {
            Hotel hotel = new Hotel();
            BeanUtils.copyProperties(hotelDto, hotel);
            if (hotelService.save(hotel)) {
                message = "保存成功";
            } else {
                message = "保存失败";
            }
        }
        return Result.builder().code(com.jade.elasticsearchdb.utils.HttpStatus.OK.getCode()).data(message).build();
    }

    @GetMapping("/del/{hotelId}")
    public Result<Object> delHotel(@PathVariable("hotelId") Long hotelId) {
        Hotel one = hotelService.getOne(new QueryWrapper<Hotel>().eq("id", hotelId));
        String message = "";
        if (one == null) {
            message = "没有找到相关记录";
        } else {
            hotelService.removeById(hotelId);
            message = "删除成功";
        }
        return Result.builder().code(HttpStatus.OK.getCode()).data(message).build();
    }

    @GetMapping("/get/{hotelId}")
    public Result<Object> getHotel(@PathVariable("hotelId") Long hotelId) {
        Hotel one = hotelService.getOne(new QueryWrapper<Hotel>().eq("id", hotelId));
        Object message = "";
        if (one == null) {
            message = "没有找到相关记录";
        } else {
            message = one;
        }
        return Result.builder().code(HttpStatus.OK.getCode()).data(message).build();
    }

    @PostMapping("/saveHotel")
    public Result<Object> saveHotel(@RequestBody HotelDto hotelDto) {
        String message = "";
        Hotel one = hotelService.getOne(new QueryWrapper<Hotel>().eq("id", hotelDto.getId()));
        if (one == null) {
            message = "更新记录不存在";
        } else {
            Hotel hotel = new Hotel();
            BeanUtils.copyProperties(hotelDto, hotel);
            if (hotelService.updateById(hotel)) {
                message = "保存成功";
            } else {
                message = "保存失败";
            }
        }
        return Result.builder().code(HttpStatus.OK.getCode()).data(message).build();
    }

    @PostMapping("/list")
    public Result<Object> list(@RequestBody HotelData hotelData) {
        QueryWrapper<Hotel> wrapper = new QueryWrapper<>();
        if (hotelData.getQuery() != null) {
            HotelDto query = hotelData.getQuery();
            if (!StringUtils.isEmpty(query.getTitle())) {
                wrapper.like("title", query.getTitle());
            }
        }
        IPage<Hotel> pageList = hotelService.page(new Page<>(hotelData.getFrom(), hotelData.getSize()), wrapper);

        HotelData resData = HotelData.builder().build();

        List<HotelDto> dtoList = new ArrayList<>();
        List<Hotel> hotelList = pageList.getRecords();
        hotelList.forEach(item->{
            HotelDto hotelDto = new HotelDto();
            BeanUtils.copyProperties(item, hotelDto);
            dtoList.add(hotelDto);
        });
        resData.setData(dtoList);
        resData.setTotal(pageList.getTotal());
        return Result.builder()
                .code(HttpStatus.OK.getCode())
                .data(resData).build();
    }

    @PostMapping("/allcity")
    public Result<Object> allcity() throws IOException {
        RestHighLevelClient highLevelClient = elasticUtil.getHighLevelClient();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        TermsAggregationBuilder aggregation = AggregationBuilders.terms("city_group").field("city");
        searchSourceBuilder.aggregation(aggregation);
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms city_group = aggregations.get("city_group");
        List<? extends Terms.Bucket> buckets = city_group.getBuckets();
        List<String> cityes = new ArrayList<>();
        buckets.stream().forEach(item->{
            cityes.add(item.getKeyAsString());
        });
        return Result.builder()
                .code(HttpStatus.OK.getCode())
                .data(cityes).build();
    }

    @PostMapping("/search")
    public Result<Object> search(@RequestBody HotelData hotelData) throws IOException{
        HotelData resData = HotelData.builder().build();
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.from(hotelData.getFrom());
        sourceBuilder.size(hotelData.getSize());
        BoolQueryBuilder builder = new BoolQueryBuilder();
        if (hotelData.getQuery() != null) {
            HotelDto query = hotelData.getQuery();
            if (StrUtil.isNotBlank(query.getCity())) {
                builder.must().add(QueryBuilders.termQuery("city", query.getCity()));
            }
            if (StrUtil.isNotBlank(query.getLocation())) {
                String[] locationStr = query.getLocation().split(",");
                Double lon = Double.parseDouble(locationStr[0]);
                Double lat = Double.parseDouble(locationStr[1]);
                builder.must().add(QueryBuilders.geoDistanceQuery("location")
                    .distance(5, DistanceUnit.KILOMETERS).point(lat, lon)
                );
            }
            if (StrUtil.isNotBlank(query.getSuggestValue())) {
                log.info("关键字查询：" + query.getSuggestValue());
                // 将关键字内容写入kafka
                kafkaUtil.sendMsg(topicName, query.getSuggestValue());
                builder.must().add(QueryBuilders.matchQuery("title", query.getSuggestValue()));
            }
        }
        sourceBuilder.query(builder);
        searchRequest.source(sourceBuilder);
        RestHighLevelClient client = elasticUtil.getHighLevelClient();
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        SearchHit[] hitsHits = hits.getHits();
        resData.setTotal(hits.getTotalHits().value);
        for (SearchHit hit : hitsHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            HotelDto hotelDto = new HotelDto();
            if (sourceAsMap.get("city") != null) {
                hotelDto.setCity(StrUtil.join(",", (List)sourceAsMap.get("city")));
            }
            hotelDto.setId((Long) sourceAsMap.get("id"));
            hotelDto.setAddress((String) sourceAsMap.get("address"));
            hotelDto.setTitle((String) sourceAsMap.get("title"));
            hotelDto.setFavourablePercent((Integer) sourceAsMap.get("favourable_percent"));
            if (sourceAsMap.get("location") != null) {
                Map<String, Object> location = (Map<String, Object>) sourceAsMap.get("location");
                hotelDto.setLat((Double) location.get("lat"));
                hotelDto.setLon((Double) location.get("lon"));
            }
            if (sourceAsMap.get("star") != null) {
                hotelDto.setStar(StrUtil.join(",", (List)sourceAsMap.get("star")));
            }
            if (sourceAsMap.get("impression") != null) {
                hotelDto.setImpression(StrUtil.join(",", (List)sourceAsMap.get("impression")));
            }
            if (sourceAsMap.get("business_district") != null) {
                hotelDto.setBusinessDistrict(StrUtil.join(",", (List)sourceAsMap.get("business_district")));
            }
            hotelDto.setFullRoom((Boolean)sourceAsMap.get("full_room"));
            hotelDto.setPrice((Double)sourceAsMap.get("price"));
            if (sourceAsMap.get("pic") != null) {
                hotelDto.setPic(StrUtil.join(",", (List)sourceAsMap.get("pic")));
            }
            if (resData.getData() == null) {
                resData.setData(new ArrayList<>());
            }
            resData.getData().add(hotelDto);
        }
        return Result.builder()
                .code(HttpStatus.OK.getCode())
                .data(resData).build();
    }

    @PostMapping("/sugsearch")
    public Result<Object> suggestSearch(@RequestBody Map<String, String> params) throws Exception {
        String prefixWord = params.get("prefix");
        if (StrUtil.isBlank(prefixWord)) {
            return Result.builder()
                    .code(HttpStatus.OK.getCode())
                    .data(ListUtil.empty()).build();
        }

        SearchRequest searchRequest = new SearchRequest(SUGGEST_INDEX_NAME);
        SearchSourceBuilder builder = new SearchSourceBuilder();
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        CompletionSuggestionBuilder chineseBuilder = SuggestBuilders.completionSuggestion("chinese").prefix(prefixWord);
        suggestBuilder.addSuggestion(chineseSugName, chineseBuilder);

        CompletionSuggestionBuilder fullPinyinBuilder = SuggestBuilders.completionSuggestion("full_pinyin").prefix(prefixWord);
        suggestBuilder.addSuggestion(fullPinyinSugName, fullPinyinBuilder);

        CompletionSuggestionBuilder headPinyinBuilder = SuggestBuilders.completionSuggestion("head_pinyin").prefix(prefixWord);
        suggestBuilder.addSuggestion(headPinyinSugName, headPinyinBuilder);
        builder.suggest(suggestBuilder);
        searchRequest.source(builder);

        RestHighLevelClient highLevelClient = elasticUtil.getHighLevelClient();
        SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        Suggest suggest = searchResponse.getSuggest();
        HashSet sugSet = new HashSet();
        CompletionSuggestion chineseSuggestion = suggest.getSuggestion(chineseSugName);
        List<String> sugList=new ArrayList<String>();
        //遍历中文suggest结果
        for (CompletionSuggestion.Entry.Option option : chineseSuggestion.getOptions()) {
            Map<String, Object>  sourceMap=option.getHit().getSourceAsMap();//获取文档
            Map<String,Object> chineseText=(Map<String,Object>)sourceMap.get("chinese");//获取文档中的中文
            String sugChinese=(String)chineseText.get("input");
            if(!sugSet.contains(sugChinese)){
                sugList.add(sugChinese);
                sugSet.add(sugChinese);
            }
        }

        CompletionSuggestion fullPinyinSuggestion = suggest.getSuggestion(fullPinyinSugName);//获取suggest结果
        //遍历拼音全拼suggest结果
        for (CompletionSuggestion.Entry.Option option : fullPinyinSuggestion.getOptions()) {
            Map<String, Object>  sourceMap=option.getHit().getSourceAsMap();//获取文档
            Map<String,Object> chineseText=(Map<String,Object>)sourceMap.get("chinese");//获取文档中的中文
            String sugChinese=(String)chineseText.get("input");
            if(!sugSet.contains(sugChinese)){
                sugList.add(sugChinese);
                sugSet.add(sugChinese);
            }
        }

        CompletionSuggestion headPinyinSuggestion = suggest.getSuggestion(headPinyinSugName);//获取suggest结果
        //遍历拼音首字母suggest结果
        for (CompletionSuggestion.Entry.Option option : headPinyinSuggestion.getOptions()) {
            Map<String, Object>  sourceMap=option.getHit().getSourceAsMap();//获取文档
            Map<String,Object> chineseText=(Map<String,Object>)sourceMap.get("chinese");//获取文档中的中文
            String sugChinese=(String)chineseText.get("input");
            if(!sugSet.contains(sugChinese)){
                sugList.add(sugChinese);
                sugSet.add(sugChinese);
            }
        }
        return Result.builder()
                .code(HttpStatus.OK.getCode())
                .data(sugList).build();
    }
}
