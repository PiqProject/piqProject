package piq.piqproject.domain.reviews.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import piq.piqproject.domain.reviews.entity.ReviewEntity;
import piq.piqproject.domain.reviews.repository.ReviewRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDao {

    private final ReviewRepository reviewRepository;

    public ReviewEntity saveReview(ReviewEntity review) {
        return reviewRepository.save(review);
    }

    public Page<ReviewEntity> getReviews(Pageable pageable) {
        return reviewRepository.findAllWithUser(pageable);
    }

    public Optional<ReviewEntity> findReview(Long reviewId) {
        return reviewRepository.findByIdWithUser(reviewId);
    }
}
