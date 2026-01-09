package piq.piqproject.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.products.dto.request.ProductRequestDto;
import piq.piqproject.domain.products.dto.response.ProductResponseDto;
import piq.piqproject.domain.products.service.ProductService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 관리자 전용 - Product(상품) 관리 API
 * 법적 요구사항에 따라 관리자의 모든 행위를 중앙에서 관리하고 로깅하기 위한 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminProductController {
    private final ProductService productService;

    /**
     * 상품을 생성하는 API입니다.
     * 
     * @param user              인증된 관리자 정보
     * @param productRequestDto 상품 생성 요청 데이터
     * @return 생성된 상품 정보
     */
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody ProductRequestDto productRequestDto) {
        log.info("Request to create a Product. User: {}", user.getUsername());

        return ResponseEntity.status(201)
                .body(productService.createProduct(productRequestDto));
    }

    /**
     * 상품을 수정하는 API입니다.
     * 
     * @param user              인증된 관리자 정보
     * @param productId         수정할 상품 ID
     * @param productRequestDto 상품 수정 요청 데이터
     * @return 수정된 상품 정보
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductRequestDto productRequestDto) {
        log.info("Request to update a Product. User: {} productId: {}", user.getUsername(), productId);

        return ResponseEntity.ok(productService.updateProduct(productId, productRequestDto));
    }

    /**
     * 상품을 삭제하는 API입니다.
     * 
     * @param user      인증된 관리자 정보
     * @param productId 삭제할 상품 ID
     * @return 성공 메시지
     */
    @PostMapping("/{productId}/delete")
    public ResponseEntity<String> deleteProduct(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("productId") Long productId) {
        log.info("Request to delete a Product. User: {} productId: {}", user.getUsername(), productId);

        productService.deleteProduct(productId);
        return ResponseEntity.ok("상품 삭제가 완료되었습니다.");
    }
}
