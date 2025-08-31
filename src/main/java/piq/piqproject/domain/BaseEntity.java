package piq.piqproject.domain;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 생성일, 수정일을 자동으로 관리하는 기반 클래스.
 */
@Getter
@jakarta.persistence.MappedSuperclass // JPA 엔티티 클래스들이 이 클래스를 상속할 경우 필드들도 칼럼으로 인식하도록 함.
@jakarta.persistence.EntityListeners(AuditingEntityListener.class) // 이 클래스에 Auditing 기능을 포함시킴.
public abstract class BaseEntity {

    // 엔티티가 생성되어 저장될 때 시간이 자동 저장됩니다.
    @CreatedDate
    @jakarta.persistence.Column(name = "created_at", updatable = false) // updatable = false: 생성 시간은 수정되지 않도록 설정
    private LocalDateTime createdAt;

    // 조회한 엔티티의 값을 변경할 때 시간이 자동 저장됩니다.
    @LastModifiedDate
    @jakarta.persistence.Column(name = "updated_at")
    private LocalDateTime updatedAt;
}