package piq.piqproject.domain.matches.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.users.entity.UserEntity;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import piq.piqproject.common.util.MessageFilterUtil;

@Getter
public class MatchingResponseDto {
    private Long matchId;
    private PartnerProfileDto partner; // '상대방'의 프로필 정보
    private MatchingStatus matchingStatus;
    private String message;

    @Builder
    public MatchingResponseDto(Long matchId, PartnerProfileDto partner,
            MatchingStatus matchingStatus, String message) {
        this.matchId = matchId;
        this.partner = partner;
        this.matchingStatus = matchingStatus;
        this.message = message;
    }

    public static MatchingResponseDto from(MatchingEntity matching, Long currentUserId) {
        // '상대방'이 누구인지 판별하는 로직
        UserEntity partner = matching.getSender().getId().equals(currentUserId)
                ? matching.getReceiver()
                : matching.getSender();

        String displayMessage = matching.getMessage();
        // 내가 수신자(receiver)인 경우에만 메시지 검열 수행
        if (matching.getReceiver().getId().equals(currentUserId) && displayMessage != null) {
            displayMessage = MessageFilterUtil.censorMessage(displayMessage);
        }

        return MatchingResponseDto.builder()
                .matchId(matching.getMatchId())
                .partner(PartnerProfileDto.from(partner))
                .matchingStatus(matching.getStatus())
                .message(displayMessage)
                .build();
    }

}
