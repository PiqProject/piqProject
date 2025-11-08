package piq.piqproject.domain.products.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.products.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findById(Long id);

}
