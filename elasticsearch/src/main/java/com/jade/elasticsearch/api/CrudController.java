package com.jade.elasticsearch.api;

import com.jade.elasticsearch.domain.ArticleEsDomain;
import com.jade.elasticsearch.repo.ArticleESRepository;
import com.jade.elasticsearch.util.EsUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/article")
@Slf4j
public class CrudController {
    @Resource
    private ArticleESRepository articleESRepository;

    @GetMapping("write2Es")
    public String write2Es() {
        log.info("写入中。。。");
        List<String> articles = new ArrayList<>();
        articles.add("-");
        articles.add("+");
        articles.add("=");
        Date date = new Date();

        List<ArticleEsDomain> collect = articles.stream().map(item -> {
                    ArticleEsDomain domain = new ArticleEsDomain();
                    domain.setTitle("item.getTitle()");
                    domain.setId("item.getId()");
                    domain.setKeywords("item.getKeywords()");
                    domain.setCategory1("item.getCategoryLevel1()");
                    domain.setCategory2("item.getCategoryLevel2()");
                    domain.setCategory3("item.getCategoryLevel3()");
                    domain.setCategory4("item.getCategoryLevel4()");
                    domain.setPlainText("item.getContent()");
                    domain.setWriteTime(date);
                    return domain;
                }).filter(item -> StringUtils.hasText(item.getPlainText()))
                .collect(Collectors.toList());

        elasticsearchRestTemplate.save(collect, IndexCoordinates.of("articles"));
        log.info("写入成功");
        return "写入成功";
    }

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @PutMapping
    public ArticleEsDomain save(@RequestBody ArticleEsDomain domain) {
        return articleESRepository.save(domain);
    }

    @GetMapping
    public SearchHits<ArticleEsDomain> select(@RequestParam(required = false) String title, @RequestParam(required = false) String category1) {


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        boolQueryBuilder.should(QueryBuilders.matchQuery("title", title));
        boolQueryBuilder.should(QueryBuilders.matchQuery("plainText", title));


        if (StringUtils.hasText(category1)) {
            boolQueryBuilder.must(QueryBuilders.termQuery("category1", category1));
        }


        log.warn("dsl:{}", boolQueryBuilder);


        ArrayList<String> highlightFields = new ArrayList<>(3);
        highlightFields.add("title");
        highlightFields.add("plainText");
        highlightFields.add("keywords");

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withHighlightBuilder(EsUtil.getHighlightBuilder(highlightFields, 4000, 0, false))
                .build();


        SearchHits<ArticleEsDomain> articles = elasticsearchRestTemplate.search(query, ArticleEsDomain.class);


        List<ArticleEsDomain> articleEsDomains = EsUtil.mappingHighlight(articles.getSearchHits());

        return articles;

    }

}
