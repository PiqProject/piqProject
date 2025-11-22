package piq.piqproject.domain.users.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.traits.entity.TraitOptionEntity;

@Getter
public class UserTraitResponseDto implements Listable {

    private Long traitOptionId;
    private String categoryName;
    private String optionName;

    @Builder
    public UserTraitResponseDto(Long traitOptionId, String categoryName, String optionName) {
        this.traitOptionId = traitOptionId;
        this.categoryName = categoryName;
        this.optionName = optionName;
    }

    /**
     * TraitOptionEntity로부터 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
     * 
     * @param traitOption TraitOption 엔티티
     * @return 생성된 DTO
     */
    public static UserTraitResponseDto from(TraitOptionEntity traitOption) {
        return UserTraitResponseDto.builder()
                .traitOptionId(traitOption.getId())
                .categoryName(traitOption.getCategory().getName()) // 지연 로딩 주의
                .optionName(traitOption.getName())
                .build();
    }
}