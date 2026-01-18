package piq.piqproject.domain.reports.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.reports.dto.request.ReportCreateRequestDto;
import piq.piqproject.domain.reports.entity.ReportEntity;
import piq.piqproject.domain.reports.repository.ReportRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository; // 유저 조회를 위해 필요

    @Transactional
    public Long createReport(Long reporterId, ReportCreateRequestDto request) {
        // 1. 신고자 조회
        UserEntity reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "존재하지 않는 사용자입니다."));

        // 2. 신고 대상 조회
        UserEntity reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "신고 대상이 존재하지 않습니다."));

        // 3. 엔티티 생성
        ReportEntity report = ReportEntity.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        // 4. 저장
        ReportEntity savedReport = reportRepository.save(report);

        return savedReport.getId();
    }
}