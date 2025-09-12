package piq.piqproject.domain.reviews.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.ForbiddenException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.reviews.dao.ReviewDao;
import piq.piqproject.domain.reviews.dto.ReviewRequestDto;
import piq.piqproject.domain.reviews.dto.ReviewResponseDto;
import piq.piqproject.domain.reviews.entity.ReviewEntity;
import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.entity.UserEntity;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.function.Predicate;

import static piq.piqproject.common.error.exception.ErrorCode.*;

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

        if (reviewDao.existsByUserEmail(email)) {
            throw new ConflictException(ALREADY_EXISTS_REVIEW);
        }

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

    @Transactional(readOnly = true)
    public ReviewResponseDto findMyReview(String email) {
        ReviewEntity review = reviewDao.findByUserEmail(email)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_REVIEW));

        return ReviewResponseDto.toDto(
                review.getId(),
                email,
                review.getTitle(),
                review.getContent(),
                review.getRate(),
                review.getCreatedAt().format(DateTimeFormatter.ISO_DATE)
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

    /**
     * @param email            인증된 유저 이메일
     * @param reviewRequestDto 수정된 리뷰 정보
     * @param reviewId         수정할 리뷰 아이디
     * @return 최종 수정된 리뷰 정보
     * <p>
     * [논의 사항]
     * 문제점: 현재 userDao를 통해 User 엔티티를 조회할 때 orElseThrow 로직이 여러 곳에서 중복적으로 사용
     * 해결 방안?: 유저 조회 시 예외 처리를 담당하는 전용 메서드를 UserService 내부에 구현하고,
     * 이를 다른 서비스 로직에서 호출하여 사용하는 방식도 좋을 것 같음.
     */
    @Transactional
    public ReviewResponseDto updateReview(String email, ReviewRequestDto reviewRequestDto, Long reviewId) {
        UserEntity user = userDao.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        ReviewEntity review = reviewDao.findReview(reviewId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_REVIEW));

        //본인 리뷰만 수정 가능
        if (!review.getUser().getUsername().equals(user.getUsername())) {
            throw new ForbiddenException(NOT_REVIEW_OWNER);
        }

        review.updateReview(reviewRequestDto.getTitle(), reviewRequestDto.getContent(), reviewRequestDto.getRate());

        return ReviewResponseDto.toDto(
                review.getId(),
                user.getUsername(),
                review.getTitle(),
                review.getContent(),
                review.getRate(),
                review.getCreatedAt().format(DateTimeFormatter.ISO_DATE)
        );
    }

    /**
     * 어드민 권한을 가진 사용자는 모든 리뷰를 자유롭게 삭제할 수 있습니다.
     * 일반 사용자(어드민 권한이 없는 사용자)는 자신이 작성한 리뷰만 삭제할 수 있습니다.
     */
    @Transactional
    public void deleteReview(String email, Collection<? extends GrantedAuthority> authorities, Long reviewId) {
        UserEntity user = userDao.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        ReviewEntity review = reviewDao.findReview(reviewId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_REVIEW));

        //유저일 경우에만 아래의 조건 검증 (todo: 추후 authService부분으로 빼는게 좋을 듯)
        boolean isUser = authorities.stream()
                .map(GrantedAuthority::getAuthority) //유저의 모든 role 정보 가져오기
                .anyMatch(Predicate.isEqual("ROLE_USER"));

        //본인 리뷰만 삭제 가능
        if (!review.getUser().getUsername().equals(user.getUsername()) && isUser) {
            throw new ForbiddenException(NOT_REVIEW_OWNER);
        }

        reviewDao.deleteReview(review);
    }
}
