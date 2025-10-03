package piq.piqproject.domain.interests.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyword;

    @Builder
    private InterestEntity (String keyword) {
        this.keyword = keyword;
    }

    public static InterestEntity of (String keyword) {
        return InterestEntity.builder()
                .keyword(keyword)
                .build();
    }

    public void updateKeyword(String keyword) {
        this.keyword = keyword;
    }
}
