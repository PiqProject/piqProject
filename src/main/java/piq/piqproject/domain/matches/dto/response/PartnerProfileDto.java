package piq.piqproject.domain.matches.dto.response;

import lombok.Getter;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.users.entity.UserEntity;

import java.util.List;

@Getter
public class PartnerProfileDto {
    private final Long userId;
    private final String nickname;
    private final String imageUrl;

    public PartnerProfileDto(UserEntity user) {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.imageUrl = extractRepresentImageUrl(user.getImages());
    }

    /**
     * 사용자의 이미지 목록에서 대표 이미지 URL을 추출하는 헬퍼 메서드.
     * 1. '메인'으로 설정된 이미지를 찾습니다.
     * 2. 메인 이미지가 없으면, 첫 번째 이미지를 대표 이미지로 사용합니다.
     * 3. 이미지가 아예 없으면, 기본 이미지 URL을 반환합니다.
     */
    private String extractRepresentImageUrl(List<UserImageEntity> images) {
        if (images == null || images.isEmpty()) {
            // TODO : 기본 이미지 URL 설정
            return "https://example.com/default-profile.png"; // 기본 이미지 URL
        }

        // UserImageEntity에 isMainImage()와 getImageUrl() 메서드가 있다고 가정합니다.
        return images.stream()
                .filter((image) -> image.getIsMainImage()) // isMainImage()는 대표 이미지 여부를 boolean으로 반환한다고 가정
                .findFirst()
                .map(UserImageEntity::getImageUrl) // getImageUrl()은 이미지 URL 문자열을 반환한다고 가정
                .orElse(images.get(0).getImageUrl()); // 대표 이미지가 없으면 첫 번째 이미지를 사용
    }

    public static PartnerProfileDto from(UserEntity user) {
        return new PartnerProfileDto(user);
    }

}