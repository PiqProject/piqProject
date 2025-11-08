package piq.piqproject.domain.users.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.userimages.dto.UserImageResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;

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
    private ListResponseDto<UserImageResponseDto> userImages;
    private String voiceUrl;
    private ListResponseDto<UserInterestResponseDto> userInterests;
    private ListResponseDto<UserIdealResponseDto> userIdeals;

    // 생성자
    @Builder
    public MyProfileResponseDto(Long id, String nickname, String email, String kakaoTalkId, String instagramId,
            Integer age,
            Gender gender,
            String mbti, String introduce, Integer pqPoint, Boolean isActive, Double score,
            ListResponseDto<UserImageResponseDto> userImages, String voiceUrl, 
            ListResponseDto<UserInterestResponseDto> userInterests, 
            ListResponseDto<UserIdealResponseDto> userIdeals
    ) {
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
        this.userImages = userImages;
        this.voiceUrl = voiceUrl;
        this.userInterests = userInterests;
        this.userIdeals = userIdeals;
    }

    public static MyProfileResponseDto from(UserEntity userEntity) {
        // 1. UserImageEntity 리스트를 UserImageResponseDto 리스트로 변환
        List<UserImageResponseDto> imageDtoList = userEntity.getImages().stream()
                .map(UserImageResponseDto::from)
                .collect(Collectors.toList());

        // UserEntity에서 UserInterestEntity 리스트를 가져와 UserInterestResponsDto로 변환
        List<UserInterestResponseDto> interestDtoList = userEntity.getUserInterests().stream()
                .map(UserInterestResponseDto::of)
                .toList();
        
        // UserEntity에서 UserIdealEntity 리스트를 가져와 UserIdealResponsDto로 변환
        List<UserIdealResponseDto> idealDtoList = userEntity.getUserIdeals().stream()
                .map(UserIdealResponseDto::of)
                .toList();


        // 2. 변환된 DTO 리스트를 ListResponseDto로 감싸기
        ListResponseDto<UserImageResponseDto> imageListResponse = ListResponseDto.from(imageDtoList);
        ListResponseDto<UserInterestResponseDto> interestListResponse = ListResponseDto.from(interestDtoList);
        ListResponseDto<UserIdealResponseDto> idealListResponse = ListResponseDto.from(idealDtoList);

        // 3. 최종 DTO 빌드
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
                .score(userEntity.getAverageScore())
                .userImages(imageListResponse)
                .voiceUrl(userEntity.getVoiceUrl())
                .userInterests(interestListResponse)
                .userIdeals(idealListResponse)
                .build();
    }
}
