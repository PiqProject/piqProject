package piq.piqproject.domain.users.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.users.entity.UserInterestEntity;

@Getter
public class UserInterestResponseDto implements Listable {
    private Long id;
    private Long userId;
    private Long interestId;

    @Builder
    private UserInterestResponseDto(Long id, Long userId, Long interestId) {
        this.id = id;
        this.userId = userId;
        this.interestId = interestId;
    }

    public static UserInterestResponseDto of(UserInterestEntity userInterestEntity) {
        return UserInterestResponseDto.builder()
                .id(userInterestEntity.getId())
                .userId(userInterestEntity.getUser().getId())
                .interestId(userInterestEntity.getInterest().getId())
                .build();

    }
}
