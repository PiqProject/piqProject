package piq.piqproject.domain.interests.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.interests.entity.InterestEntity;

public interface InterestRepository extends JpaRepository<InterestEntity, Long>{

    boolean existsByKeyword(String keyword);

}
