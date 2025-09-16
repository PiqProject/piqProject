package piq.piqproject.domain.posts.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.posts.entity.PostEntity;
import piq.piqproject.domain.posts.entity.PostType;

import java.time.format.DateTimeFormatter;

@Getter
public class PostResponseDto {
    private final Long id;

    private final String title;

    private final String content;

    private final PostType type;

    private final String startDate;

    private final String endDate;

    private final String createdAt;

    @Builder
    private PostResponseDto(Long id, String title, String content, PostType type, String startDate, String endDate, String createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public static PostResponseDto of(PostEntity post) {
        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        String formattedStartDate = post.getStartDate() != null
                ? post.getStartDate().format(DATE_TIME_FORMATTER)
                : "";

        String formattedEndDate = post.getStartDate() != null
                ? post.getEndDate().format(DATE_TIME_FORMATTER)
                : "";
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType())
                .startDate(formattedStartDate)
                .endDate(formattedEndDate)
                .createdAt(post.getCreatedAt().format(DateTimeFormatter.ISO_DATE))
                .build();
    }
}
