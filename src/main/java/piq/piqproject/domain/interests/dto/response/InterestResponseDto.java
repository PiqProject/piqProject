package piq.piqproject.domain.interests.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.interests.entity.InterestEntity;

@Getter
public class InterestResponseDto implements Listable {
    private Long id;
    private String keyword;

    @Builder
    public InterestResponseDto(Long id, String keyword) {
        this.id = id;
        this.keyword = keyword;
    }

    public static InterestResponseDto of(InterestEntity interest) {
        return InterestResponseDto.builder()
                .id(interest.getId())
                .keyword(interest.getKeyword())
                .build();
    }
}
