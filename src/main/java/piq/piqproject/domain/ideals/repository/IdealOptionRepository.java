package piq.piqproject.domain.ideals.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import piq.piqproject.domain.ideals.entity.IdealCategoryEntity;
import piq.piqproject.domain.ideals.entity.IdealOptionEntity;

public interface IdealOptionRepository extends JpaRepository<IdealOptionEntity,Long> {
    boolean existsByCategoryAndNameIn(IdealCategoryEntity category, List<String> names);

    @Query("SELECT ioe FROM IdealOptionEntity ioe JOIN FETCH ioe.category ice WHERE ice.id = :categoryId")
    List<IdealOptionEntity> findAllByCategoryId(@Param("categoryId") Long categoryId);

    List<IdealOptionEntity> findAllByCategory(IdealCategoryEntity idealCategory);
}   