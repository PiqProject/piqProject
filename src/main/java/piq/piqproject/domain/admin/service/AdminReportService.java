package piq.piqproject.domain.admin.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.reports.repository.ReportRepository;
import piq.piqproject.domain.reports.enums.ReportStatus;
import piq.piqproject.domain.reports.entity.ReportEntity;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.admin.dto.request.ReportProcessRequestDto;
import piq.piqproject.domain.admin.dto.response.ReportResponseDto;
import piq.piqproject.domain.admin.enums.ReportFilterType;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;

    // 1. 조건별 신고 목록 조회
    @Transactional
    public List<ReportResponseDto> getReportsByFilter(ReportFilterType filter) {
        List<ReportEntity> reports;

        switch (filter) {
            case UNPROCESSED:
                // 처리 대기(PENDING)거나 보류(ON_HOLD)인 것만 조회
                reports = reportRepository.findAllByStatusInOrderByCreatedAtAsc(
                        List.of(ReportStatus.PENDING, ReportStatus.ON_HOLD));
                break;

            case PROCESSED:
                // 승인(ACCEPTED)되거나 반려(REJECTED)된 것만 조회
                reports = reportRepository.findAllByStatusInOrderByCreatedAtDesc(
                        List.of(ReportStatus.ACCEPTED, ReportStatus.REJECTED));
                break;

            case ALL:
            default:
                // 전체 조회
                reports = reportRepository.findAllByOrderByCreatedAtDesc();
                break;
        }

        // Entity -> DTO 변환
        return reports.stream()
                .map(ReportResponseDto::from) // DTO에 만들어둔 static 메서드 활용
                .collect(Collectors.toList());
    }

    // 2. 신고 처리 (기존 로직 유지 + Dirty Checking)
    @Transactional
    public void processReport(Long reportId, ReportProcessRequestDto request) {
        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "존재하지 않는 신고입니다."));

        // 상태 및 코멘트 변경
        report.processReport(request.getStatus(), request.getAdminComment());
    }
}