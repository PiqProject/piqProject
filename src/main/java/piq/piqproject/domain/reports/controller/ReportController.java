package piq.piqproject.domain.reports.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import piq.piqproject.common.annotation.RequireActiveUser;
import piq.piqproject.domain.reports.dto.request.*;
import piq.piqproject.domain.reports.service.ReportService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @RequireActiveUser
    public ResponseEntity<String> createReport(
            @AuthenticationPrincipal UserEntity reporter,
            @RequestBody ReportCreateRequestDto request) {

        reportService.createReport(reporter.getId(), request);

        return ResponseEntity.ok("신고가 성공적으로 접수되었습니다.");
    }
}