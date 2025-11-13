package piq.piqproject.domain.ideals.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.ideals.entity.IdealCategoryEntity;

public interface IdealCategoryRepository extends JpaRepository<IdealCategoryEntity, Long> {
    boolean existsByName(String name);
}
