package piq.piqproject.domain.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.shop.dto.ShopRequestDto;
import piq.piqproject.domain.shop.dto.ShopResponseDto;
import piq.piqproject.domain.shop.entity.ShopEntity;
import piq.piqproject.domain.shop.repository.ShopRepository;

@Service
@RequiredArgsConstructor
public class ShopService {
    private final ShopRepository shopRepository;

    @Transactional
    public ShopResponseDto createShop(ShopRequestDto shopRequestDto) {
        ShopEntity shop = ShopEntity.of(shopRequestDto.getPrice(), shopRequestDto.getPoint());
        shopRepository.save(shop);

        return ShopResponseDto.of(shop);
    }
}
