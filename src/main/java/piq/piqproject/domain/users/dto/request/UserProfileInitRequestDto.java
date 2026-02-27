package piq.piqproject.domain.users.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.users.enums.Gender;

@Getter
@NoArgsConstructor
public class UserProfileInitRequestDto {

    @NotBlank(message = "plz write your nickname")
    @Size(min = 2, max = 10, message = "nickname must be 2~10 characters")
    private String nickname;

    @NotNull(message = "plz write your age")
    @Min(value = 18, message = "age must be 18 or older")
    private Integer age;

    @NotNull(message = "plz write your gender")
    private Gender gender; // MALE, FEMALE

    @NotBlank(message = "plz write your mbti")
    @Size(min = 4, max = 4, message = "MBTI must be 4 characters")
    private String mbti;

    @NotBlank(message = "plz write your kakaoTalkId")
    private String kakaoTalkId;

    // 인스타그램 ID는 선택사항
    private String instagramId;

    @NotBlank(message = "plz write your introduce")
    private String introduce;

    @NotBlank(message = "plz write your university")
    private String university;

    @NotBlank(message = "plz write your address")
    private String address; // 도로명 주소 (좌표 변환용)
}