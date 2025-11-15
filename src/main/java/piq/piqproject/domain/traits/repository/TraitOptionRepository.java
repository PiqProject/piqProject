package piq.piqproject.domain.traits.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.traits.entity.TraitCategoryEntity;
import piq.piqproject.domain.traits.entity.TraitOptionEntity;

public interface TraitOptionRepository extends JpaRepository<TraitOptionEntity, Long> {
    boolean existsByCategoryAndNameIn(TraitCategoryEntity category, List<String> names);

    @Query("SELECT ioe FROM TraitOptionEntity ioe JOIN FETCH ioe.category ice WHERE ice.id = :categoryId")
    List<TraitOptionEntity> findAllByCategoryId(@Param("categoryId") Long categoryId);

    List<TraitOptionEntity> findAllByCategory(TraitCategoryEntity TraitCategory);
}