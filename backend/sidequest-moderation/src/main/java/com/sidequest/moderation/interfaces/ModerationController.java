package com.sidequest.moderation.interfaces;

import com.sidequest.common.Result;
import com.sidequest.common.context.UserContext;
import com.sidequest.moderation.application.ModerationService;
import com.sidequest.moderation.infrastructure.ModerationCaseDO;
import com.sidequest.moderation.infrastructure.ReportDO;
import com.sidequest.moderation.infrastructure.SensitiveWordDO;
import com.sidequest.moderation.interfaces.dto.CheckMediaRequest;
import com.sidequest.moderation.interfaces.dto.CheckRequest;
import com.sidequest.moderation.interfaces.dto.ModerationCaseHandleRequest;
import com.sidequest.moderation.interfaces.dto.ReportRequest;
import com.sidequest.moderation.interfaces.dto.SensitiveWordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {
    private final ModerationService moderationService;

    @PostMapping("/check")
    public Result<Boolean> checkText(@Valid @RequestBody CheckRequest request) {
        return Result.success(moderationService.checkText(request.getContent()));
    }

    @PostMapping("/check-image")
    public Result<Boolean> checkImage(@Valid @RequestBody CheckMediaRequest request) {
        return Result.success(moderationService.checkImage(request.getUrl()));
    }

    @PostMapping("/check-video")
    public Result<Boolean> checkVideo(@Valid @RequestBody CheckMediaRequest request) {
        return Result.success(moderationService.checkVideo(request.getUrl()));
    }

    @GetMapping("/cases")
    public Result<List<ModerationCaseDO>> listCases(@RequestParam(required = false) Integer status) {
        return Result.success(moderationService.listCases(status));
    }

    @PostMapping("/cases/{id}/handle")
    public Result<ModerationCaseDO> handleCase(@PathVariable Long id,
                                               @Valid @RequestBody ModerationCaseHandleRequest request) {
        String operatorId = UserContext.getUserId();
        ModerationCaseDO mc = moderationService.handleCase(id, request.getResult(), request.getReason(),
                operatorId == null ? null : Long.parseLong(operatorId));
        return mc == null ? Result.error(404, "Case not found") : Result.success(mc);
    }

    @GetMapping("/sensitive-words")
    public Result<List<SensitiveWordDO>> listSensitiveWords() {
        return Result.success(moderationService.listSensitiveWords());
    }

    @PostMapping("/sensitive-words")
    public Result<SensitiveWordDO> addSensitiveWord(@Valid @RequestBody SensitiveWordRequest request) {
        return Result.success(moderationService.addSensitiveWord(request.getWord(), request.getLevel()));
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<String> deleteSensitiveWord(@PathVariable Long id) {
        moderationService.deleteSensitiveWord(id);
        return Result.success("Deleted");
    }

    @PostMapping("/reports")
    public Result<ReportDO> createReport(@Valid @RequestBody ReportRequest request) {
        String reporterId = UserContext.getUserId();
        ReportDO report = new ReportDO();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReporterId(reporterId == null ? null : Long.parseLong(reporterId));
        report.setReason(request.getReason());
        return Result.success(moderationService.createReport(report));
    }

    @GetMapping("/reports")
    public Result<List<ReportDO>> listReports(@RequestParam(required = false) Integer status) {
        return Result.success(moderationService.listReports(status));
    }

    @PostMapping("/reports/{id}/handle")
    public Result<ReportDO> handleReport(@PathVariable Long id,
                                         @RequestParam Integer status,
                                         @RequestParam(required = false) String handleResult) {
        String handlerId = UserContext.getUserId();
        ReportDO report = moderationService.handleReport(id, handleResult, status,
                handlerId == null ? null : Long.parseLong(handlerId));
        return report == null ? Result.error(404, "Report not found") : Result.success(report);
    }
}

