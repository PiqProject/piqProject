package piq.piqproject.domain.shop.entity;

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
@Table(name = "shops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int point;

    @Builder
    private ShopEntity (int price, int point) {
        this.price = price;
        this.point = point;
    }

    public static ShopEntity of(int price, int point) {
        return ShopEntity.builder()
                .price(price)
                .point(point)
                .build();
    }

    public void update(int price, int point) {
        this.price = price;
        this.point = point;
    }
}
