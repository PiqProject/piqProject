package piq.piqproject.domain.points.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.points.entity.PointHistoryEntity;
import piq.piqproject.domain.points.enums.PointType;

import static piq.piqproject.common.util.TimeUtils.formatToDateTimeWithMinutes;

@Getter
public class PointHistoryResponseDto implements Listable {

    private Long id;
    private PointType type;
    private int amount;
    private int balanceSnapshot;
    private String description;
    private String createdAt;

    @Builder
    private PointHistoryResponseDto(Long id, PointType type, int amount, int balanceSnapshot, String description,
            String createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.balanceSnapshot = balanceSnapshot;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static PointHistoryResponseDto of(PointHistoryEntity entity) {
        return PointHistoryResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .balanceSnapshot(entity.getBalanceSnapshot())
                .description(entity.getDescription())
                .createdAt(formatToDateTimeWithMinutes(entity.getCreatedAt()))
                .build();
    }
}
