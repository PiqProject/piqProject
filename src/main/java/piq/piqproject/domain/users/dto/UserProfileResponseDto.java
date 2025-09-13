package piq.piqproject.domain.users.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable; // Listable 인터페이스 import
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

    /**
     * UserEntity를 UserProfileResponseDto로 변환하는 정적 팩토리 메서드입니다.
     * 
     */
    public static UserProfileResponseDto from(UserEntity user) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .kakaoTalkId(user.getKakaoTalkId())
                .instagramId(user.getInstagramId())
                .age(user.getAge())
                .gender(user.getGender())
                .mbti(user.getMbti())
                .score(user.getScore())
                .introduce(user.getIntroduce())
                .build();
    }
}