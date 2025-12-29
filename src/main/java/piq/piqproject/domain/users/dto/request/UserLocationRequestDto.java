package piq.piqproject.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLocationRequestDto {

    @NotBlank(message = "주소를 입력해주세요.")
    private String address; // 예: "서울시 강남구 테헤란로 123"
}