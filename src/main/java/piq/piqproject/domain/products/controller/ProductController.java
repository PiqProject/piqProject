package piq.piqproject.domain.products.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.products.dto.request.ProductRequestDto;
import piq.piqproject.domain.products.dto.response.ProductResponseDto;
import piq.piqproject.domain.products.service.ProductService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody ProductRequestDto productRequestDto) {
        log.info("Request to create a Product. User: {}", user.getUsername());

        return ResponseEntity.status(201)
                .body(productService.createProduct(productRequestDto));
    }

    @GetMapping("/all")
    public ResponseEntity<ListResponseDto<ProductResponseDto>> getProducts() {
        log.info("Request to get products.");

        return ResponseEntity.ok(productService.getProducts());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductRequestDto productRequestDto) {
        log.info("Request to update a Product. User: {} productId: {}", user.getUsername(), productId);

        return ResponseEntity.ok(productService.updateProduct(productId, productRequestDto));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{productId}/delete")
    public ResponseEntity<String> deleteProduct(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("productId") Long productId) {
        log.info("Request to delete a Product. User: {} productId: {}", user.getUsername(), productId);

        productService.deleteProduct(productId);
        return ResponseEntity.ok("상품 삭제가 완료되었습니다.");
    }
}
