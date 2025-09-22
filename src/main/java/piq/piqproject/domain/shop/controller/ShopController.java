package piq.piqproject.domain.shop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.shop.dto.ShopRequestDto;
import piq.piqproject.domain.shop.dto.ShopResponseDto;
import piq.piqproject.domain.shop.service.ShopService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shops")
public class ShopController {
    private final ShopService shopService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ShopResponseDto> createShop(
        @AuthenticationPrincipal UserEntity user,
        @Valid @RequestBody ShopRequestDto shopRequestDto
    ) {
        log.info("Request to create an Shop. User: {}", user.getUsername());

        return ResponseEntity.status(201)
                    .body(shopService.createShop(shopRequestDto));
    }

}
