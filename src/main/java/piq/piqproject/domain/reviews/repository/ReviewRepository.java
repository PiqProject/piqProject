package piq.piqproject.domain.reviews.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.reviews.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
}
