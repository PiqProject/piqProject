package piq.piqproject.domain.posts.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.posts.dto.PostRequestDto;
import piq.piqproject.domain.users.entity.UserEntity;

import java.time.LocalDateTime;

@Entity
@Table(name="evnets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = true)
    private LocalDateTime startDate;

    @Column(nullable = true)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private PostType type;

    @Builder
    private PostEntity(UserEntity user, String title, String content, LocalDateTime startDate, LocalDateTime endDate, PostType type) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
    }

    public static PostEntity of(UserEntity user, PostType type, PostRequestDto postRequestDto ) {
        return PostEntity.builder()
                .title(postRequestDto.getTitle())
                .user(user)
                .content(postRequestDto.getContent())
                .type(type)
                .build();
    }
}
