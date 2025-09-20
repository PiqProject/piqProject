package piq.piqproject.domain.users.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;

@Getter
@Builder
public class UserSimpleProfileResponseDto implements Listable {

    private Long id; // 클라이언트가 상세 조회를 위해 사용할 id
    private String nickname;
    private Integer age;
    private Gender gender;
    private String mbti;
    private Double score;
    private Boolean isActive;
    private String mainImageUrl;

    /**
     * UserEntity를 UserSimpleProfileDto로 변환하는 정적 팩토리 메서드
     */
    public static UserSimpleProfileResponseDto from(UserEntity user) {
        // 사용자의 이미지 목록에서 isMainImage가 true인 첫 번째 이미지를 찾습니다.
        // 만약 대표 이미지가 없으면 null을 반환합니다. (orElse(null))
        String mainImageUrl = user.getImages().stream()
                .filter(UserImageEntity::getIsMainImage) // isMainImage가 true인 것만 필터링
                .findFirst() // 필터링된 것 중 첫 번째 것을 찾음
                .map(UserImageEntity::getImageUrl) // 찾았다면 imageUrl을 꺼냄
                .orElse(null); // 대표 이미지가 없으면 null

        return UserSimpleProfileResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .age(user.getAge())
                .gender(user.getGender())
                .mbti(user.getMbti())
                .score(user.getScore())
                .isActive(user.getIsActive())
                .mainImageUrl(mainImageUrl) // 빌더에 추가
                .build();
    }
}
