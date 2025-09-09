package piq.piqproject.domain.reviews.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import piq.piqproject.domain.reviews.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    /**
     * 모든 리뷰를 User 정보와 함께 페치 조인하여 조회합니다. (N+1 문제 해결)
     * Pageable과 fetch join을 함께 사용할 때 발생하는 in-memory 페이징 문제를 해결하기 위해 countQuery를 별도로 작성합니다.
     *
     * @param pageable 페이지네이션 정보
     * @return User 정보가 포함된 리뷰 페이지
     */
    @Query(value = "SELECT r FROM ReviewEntity r JOIN FETCH r.user u",
            countQuery = "SELECT COUNT(r) FROM ReviewEntity r")
    Page<ReviewEntity> findAllWithUser(Pageable pageable);
}
