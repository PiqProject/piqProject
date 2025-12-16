package piq.piqproject.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;

/**
 * Elasticsearch 동기화 실패 로그 엔티티
 */
@Entity
@Table(name = "sync_fail_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncFailLogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 동기화 실패한 유저 ID

    @Column(columnDefinition = "TEXT")
    private String errorReason; // 에러 메시지 (디버깅용)

    @Column(nullable = false)
    private int retryCount; // 재시도 횟수 (무한 루프 방지)

    @Column(nullable = false)
    private boolean isResolved; // 해결 여부

    @Builder
    public SyncFailLogEntity(Long userId, String errorReason) {
        this.userId = userId;
        this.errorReason = errorReason;
        this.retryCount = 0;
        this.isResolved = false;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void resolve() {
        this.isResolved = true;
    }
}