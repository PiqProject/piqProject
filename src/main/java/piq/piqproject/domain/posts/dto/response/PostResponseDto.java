package piq.piqproject.domain.posts.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import piq.piqproject.domain.posts.entity.PostEntity;
import piq.piqproject.domain.posts.entity.PostType;

import java.time.format.DateTimeFormatter;

import static piq.piqproject.common.util.TimeUtils.formatToDateTimeWithMinutes;

@Getter
@Setter
@NoArgsConstructor
public class PostResponseDto {
        private Long id;

        private String title;

        private String content;

        private PostType type;

        private String startDate;

        private String endDate;

        private String createdAt;

        @Builder
        private PostResponseDto(Long id, String title, String content, PostType type, String startDate, String endDate,
                        String createdAt) {
                this.id = id;
                this.title = title;
                this.content = content;
                this.type = type;
                this.startDate = startDate;
                this.endDate = endDate;
                this.createdAt = createdAt;
        }

        public static PostResponseDto of(PostEntity post) {
                String formattedStartDate = post.getStartDate() != null
                                ? formatToDateTimeWithMinutes(post.getStartDate())
                                : "";

                String formattedEndDate = post.getStartDate() != null
                                ? formatToDateTimeWithMinutes(post.getEndDate())
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
