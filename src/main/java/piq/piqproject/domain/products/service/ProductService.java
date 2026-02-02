package piq.piqproject.domain.products.service;

import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_PRODUCT;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.products.dto.request.ProductRequestDto;
import piq.piqproject.domain.products.dto.response.ProductResponseDto;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        ProductEntity product = ProductEntity.of(
                productRequestDto.getPrice(),
                productRequestDto.getPoint(),
                productRequestDto.getGoogleProductId(),
                productRequestDto.getAppleProductId());
        productRepository.save(product);

        return ProductResponseDto.of(product);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<ProductResponseDto> getProducts() {
        List<ProductEntity> products = productRepository.findAll();

        List<ProductResponseDto> productList = products.stream()
                .map(ProductResponseDto::of)
                .toList();

        return ListResponseDto.from(productList);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto productRequestDto) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT));

        product.update(
                productRequestDto.getPrice(),
                productRequestDto.getPoint(),
                productRequestDto.getGoogleProductId(),
                productRequestDto.getAppleProductId());

        return ProductResponseDto.of(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT));

        productRepository.delete(product);
    }
}
