package piq.piqproject.domain.users.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserScoreResponseDto {

    private String targetUserName;
    private Double score;

    @Builder
    private UserScoreResponseDto (String targetUserName, Double score) {
        this.targetUserName = targetUserName;
        this.score = score;
    }

    public static UserScoreResponseDto of (String targetUserName, Double score) {
        return UserScoreResponseDto.builder()
                .targetUserName(targetUserName)
                .score(score)
                .build();
    }
}
