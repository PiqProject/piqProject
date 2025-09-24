package piq.piqproject.domain.matches.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.matches.enums.MatchingStatus;

@Getter
@NoArgsConstructor
public class UpdateMatchingRequestDto {
    private MatchingStatus matchingStatus;
}
