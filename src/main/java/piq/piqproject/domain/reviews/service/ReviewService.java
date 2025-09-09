package piq.piqproject.domain.reviews.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.reviews.dao.ReviewDao;
import piq.piqproject.domain.reviews.dto.ReviewRequestDto;
import piq.piqproject.domain.reviews.dto.ReviewResponseDto;
import piq.piqproject.domain.reviews.entity.ReviewEntity;
import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.entity.UserEntity;

import java.time.format.DateTimeFormatter;

import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_USER;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final UserDao userDao;
    private final ReviewDao reviewDao;

    /**
     * 서비스에 대한 리뷰를 생성합니다.
     *
     * todo: 논의사항
     *
     * [추가해야하는 조건]
     * 1. 작성 자격 검증: 서비스의 핵심 기능을 경험한 사용자의 피드백을 받기 위해 최소 1회 이상 매칭을 완료했는지 확인
     * 2. 중복 작성 방지: 계정당 1회만 작성 가능하도록 제한
     *
     * [추가 논의사항]
     * 리워드 시스템: 리뷰 작성에 대한 참여를 유도하기 위해 소정의 리워드를 주는 것도 좋을 것 같음
     */
    @Transactional
    public ReviewResponseDto createReview(ReviewRequestDto reviewRequestDto, String email) {
        UserEntity user = userDao.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        ReviewEntity review = ReviewEntity.of(
                user,
                reviewRequestDto.getTitle(),
                reviewRequestDto.getContent(),
                reviewRequestDto.getRate()
        );

        ReviewEntity savedReview = reviewDao.saveReview(review);

        return ReviewResponseDto.toDto(
                savedReview.getId(),
                user.getUsername(),
                savedReview.getTitle(),
                savedReview.getContent(),
                savedReview.getRate(),
                savedReview.getCreatedAt().format(DateTimeFormatter.ISO_DATE)
        );
    }

    @Transactional(readOnly = true) //읽기만 허용
    public Page<ReviewResponseDto> getReviews(Pageable pageable) {

        //클라이언트 요청이 담긴 pageable을 넘겨줌
        Page<ReviewEntity> reviews = reviewDao.getReviews(pageable);

        //responseDto로 담아서 응답
        return reviews.map(review -> {
            return ReviewResponseDto.toDto(
                    review.getId(),
                    review.getUser().getUsername(),
                    review.getTitle(),
                    review.getContent(),
                    review.getRate(),
                    review.getCreatedAt().format(DateTimeFormatter.ISO_DATE)
            );
        });
    }
}
