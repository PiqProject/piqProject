package piq.piqproject.domain.users.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.common.list.Listable; // Listable 인터페이스 import
import piq.piqproject.domain.userimages.dto.UserImageResponseDto;
import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;

@Getter
@Builder
public class UserProfileResponseDto implements Listable {

    private final Long id;
    private final String kakaoTalkId;
    private final String instagramId;
    private final Integer age;
    private final Gender gender;
    private final String mbti;
    private final Double score;
    private final String introduce;
    private final ListResponseDto<UserImageResponseDto> userImages;

    /**
     * UserEntity를 UserProfileResponseDto로 변환하는 정적 팩토리 메서드입니다.
     */
    public static UserProfileResponseDto from(UserEntity user) {

        // 1. UserEntity에서 UserImageEntity 리스트를 가져와 UserImageResponseDto 리스트로 변환합니다.
        List<UserImageResponseDto> imageDtoList = user.getImages().stream()
                .map(UserImageResponseDto::from)
                .collect(Collectors.toList());

        // 2. 변환된 DTO 리스트를 ListResponseDto.from()을 사용하여 감싸줍니다.
        ListResponseDto<UserImageResponseDto> imageListResponse = ListResponseDto.from(imageDtoList);

        // 3. 최종적으로 UserProfileResponseDto를 빌드합니다.
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .kakaoTalkId(user.getKakaoTalkId())
                .instagramId(user.getInstagramId())
                .age(user.getAge())
                .gender(user.getGender())
                .mbti(user.getMbti())
                .score(user.getScore())
                .introduce(user.getIntroduce())
                .userImages(imageListResponse) // 완성된 ListResponseDto를 할당합니다.
                .build();
    }
}