package piq.piqproject.domain.matches.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchingRequestDto {
    @NotNull(message = "receiverId는 필수입니다.")
    private Long receiverId;
}
