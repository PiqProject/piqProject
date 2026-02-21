package piq.piqproject.domain.recommendations.dto;

import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.dto.response.UserTraitResponseDto;
import piq.piqproject.domain.users.dto.response.UserIdealResponseDto;
import lombok.Getter;
import lombok.Setter;

//TODO: 프로필 정보 추가해야함 
@Getter
@Setter
public class RecommendedUserResponseDto implements Listable {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String voiceUrl;
    private String mbti;
    private String university;
    private String address;
    private String score;
    private Integer age;
    private String introduce;
    private ListResponseDto<UserInterestResponseDto> userInterests;
    private ListResponseDto<UserTraitResponseDto> userTraits;
    private ListResponseDto<UserIdealResponseDto> userIdeals;

    public RecommendedUserResponseDto(UserEntity user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getImages().isEmpty() ? null
                : user.getImages().stream()
                        .filter(image -> image.getIsMainImage())
                        .findFirst()
                        .map(image -> image.getImageUrl())
                        .orElse(null);
        this.voiceUrl = user.getVoiceUrl();
        this.mbti = user.getMbti();
    }

}
