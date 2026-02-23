package piq.piqproject.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserAdminResponseDto { // 목록 조회용 (간략 정보)
    private Long userId;
    private String email;
    private String nickname;
    private Gender gender;
    private int age;
    private Integer pqPoint;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static UserAdminResponseDto from(UserEntity user) {
        return UserAdminResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname() != null ? user.getNickname() : "닉네임 없음")
                .gender(user.getGender() != null ? user.getGender() : Gender.OTHER)
                .age(user.getAge() != null ? user.getAge() : -1)
                .pqPoint(user.getPqPoint() != null ? user.getPqPoint() : 0)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}