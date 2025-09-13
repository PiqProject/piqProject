package piq.piqproject.domain.reviews.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ReviewResponseDto {

    private final Long reviewId;

    //리뷰에 표시할 리뷰 작성자 (todo: userEntity에 별명 추가 필요할 듯)
    private final String userName;

    private final String title;

    private final String content;

    private final int rate;

    private final String createdAt;

    @Builder
    private ReviewResponseDto(Long reviewId, String userName, String title, String content, int rate, String createdAt) {
        this.reviewId = reviewId;
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.rate = rate;
        this.createdAt = createdAt;
    }

    public static ReviewResponseDto toDto(Long reviewId, String userName, String title, String content, int rate, String createdAt) {
        return ReviewResponseDto.builder()
                .reviewId(reviewId)
                .userName(userName)
                .title(title)
                .content(content)
                .rate(rate)
                .createdAt(createdAt)
                .build();
    }
}
