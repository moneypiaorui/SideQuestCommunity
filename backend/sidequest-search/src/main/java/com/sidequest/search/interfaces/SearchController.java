package com.sidequest.search.interfaces;

import com.sidequest.common.Result;
import com.sidequest.search.domain.PostRepository;
import com.sidequest.search.infrastructure.PostDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final PostRepository postRepository;

    @GetMapping("/posts")
    public Result<List<PostDoc>> searchPosts(@RequestParam String keyword) {
        return Result.success(postRepository.findByTitleOrContent(keyword, keyword));
    }
}

