package com.sidequest.search.domain;

import com.sidequest.search.infrastructure.PostDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface PostRepository extends ElasticsearchRepository<PostDoc, String> {
    List<PostDoc> findByTitleOrContent(String title, String content);
}

