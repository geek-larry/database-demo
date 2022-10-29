package com.jade.elasticsearch.repo;

import com.jade.elasticsearch.domain.ArticleEsDomain;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleESRepository extends ElasticsearchRepository<ArticleEsDomain, String> {

    List<ArticleEsDomain> getArticleEsDomainsByTitleContains(String title, Pageable pageable);

    List<ArticleEsDomain> getArticleEsDomainByKeywordsContainsAndPlainTextContainsOrderByWriteTime(
            @Param("keywords") String keywords
            , @Param("plainText") String plainText
    );

}
