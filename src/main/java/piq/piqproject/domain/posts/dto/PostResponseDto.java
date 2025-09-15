package piq.piqproject.domain.posts.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PostResponseDto {
    private final String title;

    private final String content;

    private final String startDate;

    private final String endDate;

    private final String createdAt;

    @Builder
    private PostResponseDto(String title, String content, String startDate, String endDate, String createdAt) {
        this.title = title;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public static PostResponseDto of(String title, String content, String startDate, String endDate, String createdAt) {
        return PostResponseDto.builder()
                .title(title)
                .content(content)
                .startDate(startDate)
                .endDate(endDate)
                .createdAt(createdAt)
                .build();
    }
}
