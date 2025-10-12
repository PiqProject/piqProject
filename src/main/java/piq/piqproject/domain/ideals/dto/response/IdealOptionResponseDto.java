package piq.piqproject.domain.ideals.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.ideals.entity.IdealOptionEntity;

@Getter
public class IdealOptionResponseDto {

    private Long id;
    private String optionName;

    @Builder
    public IdealOptionResponseDto(Long id, String optionName) {
        this.id = id;
        this.optionName = optionName;
    }

    public static IdealOptionResponseDto of(IdealOptionEntity option) {
        return IdealOptionResponseDto.builder()
                .id(option.getId())
                .optionName(option.getName())
                .build();
    }
}