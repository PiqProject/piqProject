package piq.piqproject.domain.users.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 회원가입 성공 후 클라이언트에게 응답하기 위한 DTO (Data Transfer Object) 입니다.
 * Lombok의 @Builder 패턴을 사용하여 생성됩니다.
 */
@Getter
public class SignUpResponseDto {

    private final Long userId;
    private final String email;

    @Builder
    private SignUpResponseDto(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    /**
     * UserEntity 객체를 SignUpResponseDto로 변환하는 정적 팩토리 메소드입니다.
     * 내부적으로 Builder를 사용하여 객체를 생성합니다.
     *
     * @param userEntity 데이터베이스에 저장된 UserEntity 객체
     * @return SignUpResponseDto 객체
     */
    public static SignUpResponseDto toDto(UserEntity userEntity) {
        return SignUpResponseDto.builder()
                .userId(userEntity.getId())
                .email(userEntity.getEmail())
                .build();
    }
}