package piq.piqproject.domain.products.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int point;

    @Column(unique = true)
    private String googleProductId;

    @Column(unique = true)
    private String appleProductId;

    @Builder
    private ProductEntity(String name, int price, int point, String googleProductId, String appleProductId) {
        this.name = name;
        this.price = price;
        this.point = point;
        this.googleProductId = googleProductId;
        this.appleProductId = appleProductId;
    }

    public static ProductEntity of(String name, int price, int point, String googleProductId, String appleProductId) {
        return ProductEntity.builder()
                .name(name)
                .price(price)
                .point(point)
                .googleProductId(googleProductId)
                .appleProductId(appleProductId)
                .build();
    }

    public void update(String name, int price, int point, String googleProductId, String appleProductId) {
        this.name = name;
        this.price = price;
        this.point = point;
        this.googleProductId = googleProductId;
        this.appleProductId = appleProductId;
    }
}
