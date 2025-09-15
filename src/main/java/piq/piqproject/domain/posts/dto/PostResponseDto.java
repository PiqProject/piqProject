package piq.piqproject.domain.posts.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.posts.entity.PostType;

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

    public static PostResponseDto of(Long id, String title, String content, PostType type, String startDate, String endDate, String createdAt) {
        return PostResponseDto.builder()
                .id(id)
                .title(title)
                .content(content)
                .type(type)
                .startDate(startDate)
                .endDate(endDate)
                .createdAt(createdAt)
                .build();
    }
}
