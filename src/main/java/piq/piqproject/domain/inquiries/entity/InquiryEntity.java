package piq.piqproject.domain.inquiries.entity;

import jakarta.persistence.*;
import lombok.*;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.inquiries.enums.InquiryCategory;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;
import piq.piqproject.domain.users.entity.UserEntity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InquiryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자 (사용자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryCategory category;

    // 제목
    @Column(nullable = false)
    private String title;

    // 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 처리 상태 (기본값 PENDING)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.PENDING;

    // --- 답변 영역 (관리자 작성) ---

    // 답변 정보
    @Column(columnDefinition = "TEXT")
    private String answer;
    private String answeredBy;
    private LocalDateTime answeredAt;

    // --- 비즈니스 로직 ---

    // 답변 등록 및 수정 (관리자용)
    public void registerAnswer(String answer, String adminEmail) {
        this.answer = answer;
        this.answeredBy = adminEmail;
        this.answeredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        this.status = InquiryStatus.ANSWERED;
    }
}