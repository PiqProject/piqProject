package piq.piqproject.domain.traits.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.traits.entity.TraitCategoryEntity;

@Getter
public class TraitResponseDto implements Listable {
    private final Long categoryId;
    private final String categoryName;
    private final List<TraitOptionResponseDto> options;

    @Builder
    public TraitResponseDto(Long categoryId, String categoryName, List<TraitOptionResponseDto> options) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.options = options;
    }

    public static TraitResponseDto of(TraitCategoryEntity TraitCategory, List<TraitOptionResponseDto> options) {
        return TraitResponseDto.builder()
                .categoryId(TraitCategory.getId())
                .categoryName(TraitCategory.getName())
                .options(options)
                .build();
    }
}
