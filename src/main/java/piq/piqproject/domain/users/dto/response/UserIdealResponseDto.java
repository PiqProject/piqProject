package piq.piqproject.domain.users.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.users.entity.UserIdealEntity;

@Getter
public class UserIdealResponseDto implements Listable {
    private Long id;
    private Long userId;
    private Long categoryId;
    private Long optionId;
    private String categoryName;
    private String optionName;

    @Builder
    private UserIdealResponseDto(Long id, Long userId, Long categoryId, Long optionId, String categoryName, String optionName) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.optionId = optionId;
        this.categoryName = categoryName;
        this.optionName = optionName;
    }

    public static UserIdealResponseDto of(UserIdealEntity userIdeal) {
        return UserIdealResponseDto.builder()
                .id(userIdeal.getId())
                .userId(userIdeal.getUser().getId())
                .categoryId(userIdeal.getIdealOption().getCategory().getId())
                .optionId(userIdeal.getIdealOption().getId())
                .categoryName(userIdeal.getIdealOption().getCategory().getName())
                .optionName(userIdeal.getIdealOption().getName())
                .build();
    }
}
