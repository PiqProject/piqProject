package piq.piqproject.domain.products.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.products.dto.response.ProductResponseDto;
import piq.piqproject.domain.products.service.ProductService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/all")
    public ResponseEntity<ListResponseDto<ProductResponseDto>> getProducts() {
        log.info("Request to get products.");

        return ResponseEntity.ok(productService.getProducts());
    }

}
