package piq.piqproject.domain.traits.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.traits.entity.TraitOptionEntity;

@Getter
public class TraitOptionResponseDto {

    private final Long id;
    private final String optionName;

    @Builder
    public TraitOptionResponseDto(Long id, String optionName) {
        this.id = id;
        this.optionName = optionName;
    }

    public static TraitOptionResponseDto of(TraitOptionEntity option) {
        return TraitOptionResponseDto.builder()
                .id(option.getId())
                .optionName(option.getName())
                .build();
    }
}