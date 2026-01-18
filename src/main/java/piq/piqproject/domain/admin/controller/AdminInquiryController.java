package piq.piqproject.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import piq.piqproject.common.annotation.AuditLog;
import piq.piqproject.domain.admin.dto.request.InquiryAnswerRequestDto;
import piq.piqproject.domain.admin.dto.response.AdminInquiryDetailResponseDto;
import piq.piqproject.domain.admin.dto.response.AdminInquiryListResponseDto;
import piq.piqproject.domain.admin.service.AdminInquiryService;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @GetMapping
    public ResponseEntity<Page<AdminInquiryListResponseDto>> getInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AdminInquiryListResponseDto> page = adminInquiryService.getInquiries(status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<AdminInquiryDetailResponseDto> getInquiryDetail(@PathVariable("inquiryId") Long inquiryId) {
        return ResponseEntity.ok(adminInquiryService.getInquiryDetail(inquiryId));
    }

    @PutMapping("/{inquiryId}/answer")
    @AuditLog(action = "문의 답변 등록")
    public ResponseEntity<String> replyInquiry(
            @PathVariable("inquiryId") Long inquiryId,
            @RequestBody @Valid InquiryAnswerRequestDto request,
            @AuthenticationPrincipal UserEntity admin) {

        adminInquiryService.replyInquiry(inquiryId, request, admin);
        return ResponseEntity.ok("답변이 성공적으로 등록되었습니다.");
    }
}