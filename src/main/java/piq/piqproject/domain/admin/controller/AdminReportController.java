package piq.piqproject.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.admin.dto.response.ReportResponseDto;
import piq.piqproject.domain.admin.dto.request.ReportProcessRequestDto;
import piq.piqproject.domain.admin.enums.ReportFilterType;
import piq.piqproject.domain.admin.service.AdminReportService;

@RestController
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<ListResponseDto<ReportResponseDto>> getReports(
            @RequestParam(defaultValue = "UNPROCESSED") ReportFilterType filter) {
        return ResponseEntity.ok(ListResponseDto.from(adminReportService.getReportsByFilter(filter)));
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<String> processReport(
            @PathVariable("reportId") Long reportId,
            @RequestBody @Valid ReportProcessRequestDto request) {

        adminReportService.processReport(reportId, request);
        return ResponseEntity.ok("신고 처리가 완료되었습니다.");
    }
}
