package piq.piqproject.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.reports.enums.ReportStatus;

@Getter
@NoArgsConstructor
public class ReportProcessRequestDto {
    @NotNull(message = "처리 상태는 필수입니다.")
    private ReportStatus status;
    private String adminComment;
}