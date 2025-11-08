package piq.piqproject.domain.products.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.products.entity.ProductEntity;

import static piq.piqproject.common.util.TimeUtils.*;

@Getter
public class ProductResponseDto implements Listable {

    private Long id;

    private int price;

    private int point;

    private String createdAt;

    @Builder
    private ProductResponseDto(Long id, int price, int point, String createdAt) {
        this.id = id;
        this.price = price;
        this.point = point;
        this.createdAt = createdAt;
    }

    public static ProductResponseDto of(ProductEntity shop) {
        return ProductResponseDto.builder()
                .id(shop.getId())
                .price(shop.getPrice())
                .point(shop.getPoint())
                .createdAt(formatToDateTimeWithMinutes(shop.getCreatedAt()))
                .build();
    }

}
