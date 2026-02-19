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

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 이내여야 합니다.")
    private String nickname;

    @NotNull(message = "나이는 필수입니다.")
    @Min(value = 18, message = "18세 이상만 가입 가능합니다.")
    private Integer age;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender; // MALE, FEMALE

    @NotBlank(message = "MBTI는 필수입니다.")
    @Size(min = 4, max = 4, message = "MBTI는 4글자여야 합니다.")
    private String mbti;

    @NotBlank(message = "카카오톡 ID는 필수입니다.")
    private String kakaoTalkId;

    // 인스타그램 ID는 선택사항
    private String instagramId;

    private String introduce;

    @NotBlank(message = "대학교(직장)는 필수입니다.")
    private String university;

    @NotBlank(message = "주소는 필수입니다.")
    private String address; // 도로명 주소 (좌표 변환용)
}