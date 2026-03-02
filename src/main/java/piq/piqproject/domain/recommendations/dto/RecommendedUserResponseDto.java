package piq.piqproject.domain.recommendations.dto;

import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.dto.response.UserTraitResponseDto;
import piq.piqproject.domain.users.dto.response.UserIdealResponseDto;

import java.util.List;

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
        private Double score;
        private Integer age;
        private String introduce;
        private ListResponseDto<UserInterestResponseDto> userInterests;
        private ListResponseDto<UserTraitResponseDto> userTraits;
        private ListResponseDto<UserIdealResponseDto> userIdeals;
        private String recommendedStatus; // NONE, LIKED, PASSED
        private boolean actioned; // 추천 액션 수행 여부 (좋아요/패스)

        public RecommendedUserResponseDto(UserEntity user) {
                // user에서 UserInterestEntity 리스트를 가져와 UserInterestResponsDto로 변환
                List<UserInterestResponseDto> interestDtoList = user.getUserInterests().stream()
                                .map(UserInterestResponseDto::of)
                                .toList();

                // user에서 UserIdealEntity 리스트를 가져와 UserIdealResponsDto로 변환
                List<UserIdealResponseDto> idealDtoList = user.getUserIdeals().stream()
                                .map(UserIdealResponseDto::of)
                                .toList();

                // user에서 UserTraitEntity 리스트를 가져와 UserTraitResponseDto로 변환
                List<UserTraitResponseDto> traitDtoList = user.getUserTraits().stream()
                                .map((userTrait) -> UserTraitResponseDto.from(userTrait.getTraitOption()))
                                .toList();

                // 2. 변환된 DTO 리스트를 ListResponseDto로 감싸기
                ListResponseDto<UserInterestResponseDto> interestListResponse = ListResponseDto.from(interestDtoList);
                ListResponseDto<UserIdealResponseDto> idealListResponse = ListResponseDto.from(idealDtoList);
                ListResponseDto<UserTraitResponseDto> traitListResponse = ListResponseDto.from(traitDtoList);

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
                this.university = user.getUniversity();
                this.address = user.getAddress();
                this.score = user.getAverageScore();
                this.age = user.getAge();
                this.introduce = user.getIntroduce();
                this.userInterests = interestListResponse;
                this.userTraits = traitListResponse;
                this.userIdeals = idealListResponse;
                this.actioned = false; // 기본값
                this.recommendedStatus = "NONE";
        }

        public RecommendedUserResponseDto(UserEntity user, boolean actioned, String recommendedStatus) {
                this(user);
                this.actioned = actioned;
                this.recommendedStatus = recommendedStatus;
        }
}
