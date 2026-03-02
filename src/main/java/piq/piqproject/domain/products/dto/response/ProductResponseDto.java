package piq.piqproject.domain.products.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.common.list.Listable;
import piq.piqproject.domain.products.entity.ProductEntity;

import static piq.piqproject.common.util.TimeUtils.*;

@Getter
public class ProductResponseDto implements Listable {

    private Long id;

    private String name;

    private int price;

    private int point;

    private String createdAt;

    private String googleProductId;

    private String appleProductId;

    @Builder
    private ProductResponseDto(Long id, String name, int price, int point, String createdAt, String googleProductId,
            String appleProductId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.point = point;
        this.createdAt = createdAt;
        this.googleProductId = googleProductId;
        this.appleProductId = appleProductId;
    }

    public static ProductResponseDto of(ProductEntity product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .point(product.getPoint())
                .createdAt(formatToDateTimeWithMinutes(product.getCreatedAt()))
                .googleProductId(product.getGoogleProductId())
                .appleProductId(product.getAppleProductId())
                .build();
    }
}
