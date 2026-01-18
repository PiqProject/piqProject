package piq.piqproject.domain.admin.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.reports.entity.ReportEntity;
import piq.piqproject.domain.reports.enums.ReportReason;
import piq.piqproject.domain.reports.enums.ReportStatus;
import piq.piqproject.common.list.Listable;

@Getter
@Builder
@AllArgsConstructor
public class ReportResponseDto implements Listable {
    private Long reportId;
    private String reporterEmail; // 신고자
    private String reportedUserEmail; // 신고 대상
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;

    public static ReportResponseDto from(ReportEntity entity) {
        return ReportResponseDto.builder()
                .reportId(entity.getId())
                .reporterEmail(entity.getReporter().getEmail())
                .reportedUserEmail(entity.getReportedUser().getEmail())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}