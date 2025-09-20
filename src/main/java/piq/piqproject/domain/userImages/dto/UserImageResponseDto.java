package piq.piqproject.domain.userimages.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.userimages.entity.UserImageEntity;

@Getter
@Builder
public class UserImageResponseDto implements Listable {

    private Long imageId;
    private String imageUrl;
    private Boolean isMainImage;

    /**
     * UserImageEntity를 UserImageResponseDto로 변환하는 정적 팩토리 메서드
     */
    public static UserImageResponseDto from(UserImageEntity userImage) {
        return UserImageResponseDto.builder()
                .imageId(userImage.getImageId())
                .imageUrl(userImage.getImageUrl())
                .isMainImage(userImage.getIsMainImage())
                .build();
    }
}