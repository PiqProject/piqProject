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


}
