package piq.piqproject.domain.traits.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.traits.entity.TraitCategoryEntity;

public interface TraitCategoryRepository extends JpaRepository<TraitCategoryEntity, Long> {
    boolean existsByName(String name);
}
