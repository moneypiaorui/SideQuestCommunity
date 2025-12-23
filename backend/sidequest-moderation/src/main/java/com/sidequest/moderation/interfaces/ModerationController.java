package com.sidequest.moderation.interfaces;

import com.sidequest.common.Result;
import com.sidequest.moderation.application.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {
    private final ModerationService moderationService;

    @PostMapping("/check")
    public Result<Boolean> checkText(@RequestBody String text) {
        return Result.success(moderationService.checkText(text));
    }
}

