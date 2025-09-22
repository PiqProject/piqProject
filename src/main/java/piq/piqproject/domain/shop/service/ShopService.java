package piq.piqproject.domain.shop.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.shop.dto.request.ShopRequestDto;
import piq.piqproject.domain.shop.dto.response.ShopResponseDto;
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

    @Transactional(readOnly = true)
    public ListResponseDto<ShopResponseDto> getShops() {
        List<ShopEntity> shops = shopRepository.findAll();

        List<ShopResponseDto> shopList= shops.stream()
                .map(ShopResponseDto::of)
                .toList();
        
        return ListResponseDto.from(shopList);
    }
}
