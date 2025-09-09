package piq.piqproject.domain.reviews.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import piq.piqproject.domain.reviews.entity.ReviewEntity;
import piq.piqproject.domain.reviews.repository.ReviewRepository;

@Repository
@RequiredArgsConstructor
public class ReviewDao {

    private final ReviewRepository reviewRepository;

    public ReviewEntity saveReview(ReviewEntity review) {
        return reviewRepository.save(review);
    }
}
