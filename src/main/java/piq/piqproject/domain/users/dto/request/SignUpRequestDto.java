package piq.piqproject.domain.users.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import piq.piqproject.domain.users.entity.Gender;

// json으로 요청보낼때 Gender의 경우 "MALE", "FEMALE"과 같은 문자열로 전송해야함
// 필수가 아닌 값은 key - value 매핑시 value를 null로 적을 것
// message 속성: 유효성 검증에 실패했을 때 반환될 에러메시지 지정
// NotBlank: NotNull + NotSpace

@Getter
@Setter
@NoArgsConstructor
// JSON 데이터를 이 DTO 객체로 변환할 때 기본 생성자를 사용하여 객체를 생성한 후
// Setter를 통해 값을 채워넣는다
// 분변성을 원한다면 JsonCreator, JsonProperty 어노테이션 사용할것
public class SignUpRequestDto {

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식에 맞지 않습니다.")
    private String email;

    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 길이여야 합니다.")
    private String nickname;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$", message = "비밀번호는 8~20자 길이의 영문, 숫자, 특수문자 조합이어야 합니다.")
    private String password;

    @NotBlank(message = "카카오톡 ID는 필수 입력 값입니다.")
    private String kakaoTalkId;

    private String instagramId;

    @NotNull(message = "나이는 필수 입력 값입니다.")
    @Positive(message = "나이는 양수여야 합니다.")
    private Integer age;

    @NotNull(message = "성별은 필수 선택 값입니다.")
    private Gender gender;

    @Size(max = 4, message = "MBTI는 4글자여야 합니다.")
    private String mbti;

    @NotBlank(message = "자기소개는 필수 입력 값입니다.")
    private String introduce;
}