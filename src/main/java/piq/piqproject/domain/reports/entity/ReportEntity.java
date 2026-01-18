package piq.piqproject.domain.reports.entity;

import jakarta.persistence.*;
import lombok.*;
import piq.piqproject.domain.reports.enums.ReportReason;
import piq.piqproject.domain.reports.enums.ReportStatus;
import piq.piqproject.domain.users.entity.UserEntity; // User 엔티티 import
import piq.piqproject.domain.BaseEntity; // BaseEntity가 있다면 상속

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고한 사람 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    // 신고 당한 사람 (다대일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private UserEntity reportedUser;

    // 신고사유
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    // 상세 사유: reason.other일 경우에만 사용
    @Column(columnDefinition = "TEXT")
    private String description;

    // 상태
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;

    // 관리자 처리 코멘트 (특이사항 메모)
    private String adminComment;

    // 비즈니스 로직: 신고 처리 상태 변경
    public void processReport(ReportStatus newStatus, String comment) {
        this.status = newStatus;
        this.adminComment = comment;
    }

    @Builder
    public ReportEntity(UserEntity reporter, UserEntity reportedUser, ReportReason reason,
            String description) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reason = reason;
        this.description = description;
    }

}