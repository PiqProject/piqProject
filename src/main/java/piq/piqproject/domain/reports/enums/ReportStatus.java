package piq.piqproject.domain.reports.enums;

public enum ReportStatus {
    PENDING, // 접수 대기
    ON_HOLD, // 보류
    ACCEPTED, // 처리 완료 (제재함)
    REJECTED // 반려 (문제 없음)
}