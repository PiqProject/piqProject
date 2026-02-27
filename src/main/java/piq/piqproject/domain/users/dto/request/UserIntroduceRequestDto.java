package piq.piqproject.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserIntroduceRequestDto {
    @NotBlank(message = "plz write your introduce")
    private String introduce;
}
