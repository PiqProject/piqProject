package piq.piqproject.domain.users.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;

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

    /**
     * UserEntity를 UserSimpleProfileDto로 변환하는 정적 팩토리 메서드
     */
    public static UserSimpleProfileResponseDto from(UserEntity user) {
        return UserSimpleProfileResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .age(user.getAge())
                .gender(user.getGender())
                .mbti(user.getMbti())
                .score(user.getScore())
                .isActive(user.getIsActive())
                .build();
    }
}