package piq.piqproject.domain.verification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.enums.VerificationStatus;

@Table(name = "verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class VerificationEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private String contentValue;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status;

    @Builder
    private VerificationEntity(UserEntity user, ContentType contentType, String contentValue,
            VerificationStatus status) {
        this.user = user;
        this.contentType = contentType;
        this.contentValue = contentValue;
        this.status = status;
    }

    public static VerificationEntity of(UserEntity user, ContentType contentType, String contentValue,
            VerificationStatus status) {
        return VerificationEntity.builder()
                .user(user)
                .contentType(contentType)
                .contentValue(contentValue)
                .status(status)
                .build();
    }

    public void reject() {
        this.status = VerificationStatus.REJECTED;
    }

    public void approve() {
        this.status = VerificationStatus.APPROVED;
    }
}