package piq.piqproject.domain.matches.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.users.entity.UserEntity;

@Getter
public class MatchingResponseDto {
    private Long matchId;
    private PartnerProfileDto partner; // '상대방'의 프로필 정보
    private MatchingStatus matchingStatus;

    @Builder
    public MatchingResponseDto(Long matchId, PartnerProfileDto partner,
            MatchingStatus matchingStatus) {
        this.matchId = matchId;
        this.partner = partner;
        this.matchingStatus = matchingStatus;
    }

    public static MatchingResponseDto from(MatchingEntity matching, Long currentUserId) {
        // '상대방'이 누구인지 판별하는 로직
        UserEntity partner = matching.getSender().getId().equals(currentUserId)
                ? matching.getReceiver()
                : matching.getSender();

        return MatchingResponseDto.builder()
                .matchId(matching.getMatchId())
                .partner(PartnerProfileDto.from(partner))
                .matchingStatus(matching.getStatus())
                .build();
    }
}
