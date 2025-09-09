package piq.piqproject.domain.reviews.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.users.entity.UserEntity;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //todo: user 탈퇴 시 review 삭제 필요 (유저 삭제 시 리뷰 삭제 로직 추가)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = true, length = 255)
    private String content;

    @Column(nullable = false)
    private int rate;

    @Builder
    private ReviewEntity(UserEntity user, String title, String content, int rate) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.rate = rate;
    }

    public static ReviewEntity of(UserEntity user, String title, String content, int rate) {
        return ReviewEntity.builder()
                .user(user)
                .title(title)
                .content(content)
                .rate(rate)
                .build();
    }
}
