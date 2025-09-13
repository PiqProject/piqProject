package piq.piqproject.domain.reviews.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.reviews.dto.ReviewRequestDto;
import piq.piqproject.domain.reviews.dto.ReviewResponseDto;
import piq.piqproject.domain.reviews.service.ReviewService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * @summary 서비스에 대한 리뷰 작성 API
     * @description 사용자가 서비스 이용 경험에 대한 후기를 작성합니다. ['매칭 상대방'에 대한 평가(신뢰도)는 별도의 API를 통해 관리해야할 것 같음]
     *
     * PreAuthorize의 경우 메서드 실행 전 로그인된 사용자의 인증 과정을 거칩니다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(reviewRequestDto, userDetails.getUsername()));
    }

    /**
     * @summary 본인이 작성한 리뷰 조회
     * todo: 매칭 상대방에 대한 평가 내역 조회 추가 필요 (추후 구현)
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ResponseEntity<ReviewResponseDto> findMyReview(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reviewService.findMyReview(userDetails.getUsername()));
    }

    /**
     * @param pageable 페이지네이션 정보 (page, size, sort)
     * @return 페이지네이션된 리뷰 목록
     * @summary 서비스에 대한 리뷰 전체 조회 API
     *
     * 클라이언트에서 별도의 페이지네이션 파라미터를 지정하지 않을 경우
     * default 첫 페이지(0), 페이지당 10개, 최신순으로 정렬하여 반환합니다.
     */
    @GetMapping
    public ResponseEntity<Page<ReviewResponseDto>> getReviews(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviews(pageable));
    }

    /**
     * @param userDetails      인증된 유저 정보
     * @param reviewRequestDto 수정된 리뷰 정보
     * @param reviewId         수정할 리뷰 아이디
     * @return //수정 완료된 리뷰 정보
     * @summary 리뷰 수정 API
     *
     * PutMapping 사용 이유
     * 클라이언트 측에서 변경되지 않은 정보도 함께 전달하여 기존 데이터를 모두 덮어쓰는 PUT 방식을 채택하였습니다.
     * 이는 관리 효율성을 높이고, 향후 필드가 많아질 경우 PATCH 방식에서 발생할 수 있는 변경되지 않은 필드에 대한 비즈니스 로직 예외 처리 부담을 줄이고자 함입니다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequestDto reviewRequestDto,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(reviewService.updateReview(userDetails.getUsername(), reviewRequestDto, reviewId));
    }

    /**
     * @param userDetails 인증된 유저 정보
     * @param reviewId    삭제할 리뷰 아이디
     * @return 성공 메세지
     *
     * 사용자가 부적절한 리뷰를 작성하는 경우를 대비하여 admin도 삭제 기능을 사용할 수 있도록 하였습니다.
     */
    @PostMapping("/{reviewId}/delete")
    public ResponseEntity<String> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(userDetails.getUsername(), userDetails.getAuthorities(), reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }
}
