package piq.piqproject.domain.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 요청을 처리하기 위한 DTO (Data Transfer Object) 입니다.
 * 클라이언트로부터 이메일과 비밀번호를 받습니다.
 */
@Getter
@Setter
@NoArgsConstructor // JSON -> DTO 객체 변환 시 Jackson 라이브러리가 기본 생성자를 사용합니다.
public class LoginRequestDto {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
