package piq.piqproject.domain.shop.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 가격과 포인트는 추후 범위 제한 생각한 후 기입
 */
@Getter
public class ShopRequestDto {
    @NotNull(message = "가격을 입력해주세요.")
    private int price;

    @NotNull(message = "포인트를 입력해주세요.")
    private int point;
}
