package com.sidequest.search.domain;

import com.sidequest.search.infrastructure.PostDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface PostRepository extends ElasticsearchRepository<PostDoc, String> {
    @Query("{\"bool\": {\"must\": [{\"term\": {\"status\": 1}}, {\"bool\": {\"should\": [{\"match\": {\"title\": \"?0\"}}, {\"term\": {\"tags\": \"?0\"}}], \"minimum_should_match\": 1}}]}}")
    Page<PostDoc> findByKeyword(String keyword, Pageable pageable);
    
    Page<PostDoc> findByAuthorIdAndStatus(Long authorId, Integer status, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"term\": {\"status\": 1}}, {\"match\": {\"authorName\": \"?0\"}}]}}")
    Page<PostDoc> findByAuthorNameKeyword(String keyword, Pageable pageable);

    Page<PostDoc> findByStatus(Integer status, Pageable pageable);

    Page<PostDoc> findBySectionIdAndStatus(Long sectionId, Integer status, Pageable pageable);
}

