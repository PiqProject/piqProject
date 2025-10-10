package piq.piqproject.domain.ideals.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.ideals.entity.IdealCategoryEntity;
import piq.piqproject.domain.ideals.entity.IdealOptionEntity;

public interface IdealOptionRepository extends JpaRepository<IdealOptionEntity,Long> {
    boolean existsByCategoryAndNameIn(IdealCategoryEntity category, List<String> names);
} 