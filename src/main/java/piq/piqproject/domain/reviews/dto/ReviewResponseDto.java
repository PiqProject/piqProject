package piq.piqproject.domain.reviews.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import piq.piqproject.domain.reviews.entity.ReviewEntity;

import static piq.piqproject.common.util.TimeUtils.formatToDate;

@Getter
@Setter
@NoArgsConstructor
public class ReviewResponseDto {

    private Long reviewId;

    private String nickName;

    private String title;

    private String content;

    private int rate;

    private String createdAt;

    @Builder
    private ReviewResponseDto(Long reviewId, String nickName, String title, String content, int rate,
            String createdAt) {
        this.reviewId = reviewId;
        this.nickName = nickName;
        this.title = title;
        this.content = content;
        this.rate = rate;
        this.createdAt = createdAt;
    }

    public static ReviewResponseDto of(ReviewEntity review) {
        return ReviewResponseDto.builder()
                .reviewId(review.getId())
                .nickName(review.getUser().getNickname())
                .title(review.getTitle())
                .content(review.getContent())
                .rate(review.getRate())
                .createdAt(formatToDate(review.getCreatedAt()))
                .build();
    }
}
