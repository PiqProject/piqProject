package piq.piqproject.domain.reports.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.reports.enums.ReportReason;

@Getter
@NoArgsConstructor
public class ReportCreateRequestDto {

    @NotNull(message = "신고 대상은 필수입니다.")
    private Long reportedUserId;

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    private String description;
}