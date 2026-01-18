package piq.piqproject.domain.inquiries.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.inquiries.dto.request.InquiryCreateRequestDto;
import piq.piqproject.domain.inquiries.dto.response.InquiryDetailResponseDto;
import piq.piqproject.domain.inquiries.dto.response.InquiryListResponseDto;
import piq.piqproject.domain.inquiries.service.InquiryService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<String> createInquiry(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody @Valid InquiryCreateRequestDto request) {

        inquiryService.createInquiry(user, request);
        return ResponseEntity.ok("문의가 성공적으로 등록되었습니다.");
    }

    @GetMapping
    public ResponseEntity<Page<InquiryListResponseDto>> getMyInquiries(
            @AuthenticationPrincipal UserEntity user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InquiryListResponseDto> page = inquiryService.getMyInquiries(user, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDetailResponseDto> getInquiryDetail(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("inquiryId") Long inquiryId) {

        return ResponseEntity.ok(inquiryService.getInquiryDetail(user, inquiryId));
    }
}