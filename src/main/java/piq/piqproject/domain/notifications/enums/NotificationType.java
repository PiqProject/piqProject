package piq.piqproject.domain.notifications.enums;

public enum NotificationType {

    // === 콘텐츠 검증 관련 ===
    CONTENT_SUBMITTED, // 콘텐츠(사진/음성/소개글) 제출 완료 (검증 대기)
    CONTENT_APPROVED, // 콘텐츠 승인
    CONTENT_REJECTED, // 콘텐츠 거절

    // === 매칭 관련 ===
    MATCH_RECEIVED, // 새로운 매칭 요청 도착 (Receiver에게)
    MATCH_SUCCESS, // 매칭 성공 (Sender에게)
    MATCH_FAIL, // 매칭 실패/거절 (Sender에게)

    // === 공지/이벤트 ===
    ANNOUNCEMENT, // 공지사항
    EVENT // 이벤트
}
