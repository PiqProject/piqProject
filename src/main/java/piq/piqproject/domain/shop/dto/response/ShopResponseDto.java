package piq.piqproject.domain.shop.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.shop.entity.ShopEntity;
import static piq.piqproject.common.util.TimeUtils.*;

@Getter
public class ShopResponseDto implements Listable{

    private Long id;

    private int price;

    private int point;

    private String createdAt;

    @Builder 
    private ShopResponseDto(Long id, int price, int point, String createdAt) {
        this.id = id;
        this.price = price;
        this.point = point;
        this.createdAt = createdAt;
    }

    public static ShopResponseDto of(ShopEntity shop) {
        return ShopResponseDto.builder()
                    .id(shop.getId())
                    .price(shop.getPrice())
                    .point(shop.getPoint())
                    .createdAt(formatToDateTimeWithMinutes(shop.getCreatedAt()))
                    .build();
    }

}
