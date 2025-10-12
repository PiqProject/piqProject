package piq.piqproject.domain.ideals.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.ideals.entity.IdealCategoryEntity;

@Getter
public class IdealResponseDto implements Listable {
    private final Long categoryId;
    private final String categoryName;
    private final List<IdealOptionResponseDto> options;

    @Builder
    public IdealResponseDto(Long categoryId, String categoryName, List<IdealOptionResponseDto> options) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.options = options;
    }

    public static IdealResponseDto of(IdealCategoryEntity idealCategory, List<IdealOptionResponseDto> options) {
        return IdealResponseDto.builder()
                .categoryId(idealCategory.getId())
                .categoryName(idealCategory.getName())
                .options(options)
                .build();
    }
}
