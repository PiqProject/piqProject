package piq.piqproject.domain.admin.enums;

public enum ReportFilterType {
    ALL, // 전체 보기
    UNPROCESSED, // 미처리 (PENDING, ON_HOLD) - 관리자가 작업해야 할 것들
    PROCESSED // 처리 완료 (ACCEPTED, REJECTED) - 기록용
}