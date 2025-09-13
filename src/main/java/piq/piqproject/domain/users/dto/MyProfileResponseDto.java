package piq.piqproject.domain.users.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;

@Getter
public class MyProfileResponseDto {
    private Long id;
    private String nickname;
    private String email;
    private Integer age;
    private Gender gender;
    private String mbti;
    private String introduce;
    private String kakaoTalkId;
    private String instagramId;
    private Integer pqPoint;
    private Double score;
    private Boolean isActive;

    // 생성자
    @Builder
    public MyProfileResponseDto(Long id, String nickname, String email, String kakaoTalkId, String instagramId,
            Integer age,
            Gender gender,
            String mbti, String introduce, Integer pqPoint, Boolean isActive, Double score) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.kakaoTalkId = kakaoTalkId;
        this.instagramId = instagramId;
        this.age = age;
        this.gender = gender;
        this.mbti = mbti;
        this.introduce = introduce;
        this.pqPoint = pqPoint;
        this.isActive = isActive;
        this.score = score;
    }

    public static MyProfileResponseDto from(UserEntity userEntity) {
        return MyProfileResponseDto.builder()
                .id(userEntity.getId())
                .nickname(userEntity.getNickname())
                .email(userEntity.getEmail())
                .kakaoTalkId(userEntity.getKakaoTalkId())
                .instagramId(userEntity.getInstagramId())
                .age(userEntity.getAge())
                .gender(userEntity.getGender())
                .mbti(userEntity.getMbti())
                .introduce(userEntity.getIntroduce())
                .pqPoint(userEntity.getPqPoint())
                .isActive(userEntity.getIsActive())
                .score(userEntity.getScore())
                .build();
    }
}
