package piq.piqproject.domain.matches.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.matches.enums.MatchingStatus;

@Getter
@NoArgsConstructor
public class UpdateMatchingRequestDto {
    @NotNull(message = "matchingStatus는 필수입니다.")
    private MatchingStatus matchingStatus;
}
