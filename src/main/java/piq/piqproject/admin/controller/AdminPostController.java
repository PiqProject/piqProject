package piq.piqproject.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.posts.dto.request.AnnouncementRequestDto;
import piq.piqproject.domain.posts.dto.request.EventRequestDto;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.service.PostService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 관리자 전용 - Post(공지사항/이벤트) 관리 API
 * 법적 요구사항에 따라 관리자의 모든 행위를 중앙에서 관리하고 로깅하기 위한 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/posts")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminPostController {
    private final PostService postService;

    /**
     * 공지사항을 생성하는 API입니다.
     * 
     * @param user                   인증된 관리자 정보
     * @param announcementRequestDto 공지사항 생성 요청 데이터
     * @return 생성된 공지사항 정보
     */
    @PostMapping("/announcement")
    public ResponseEntity<PostResponseDto> createAnnouncement(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody AnnouncementRequestDto announcementRequestDto) {
        log.info("Request to create an announcement. User: {}", user.getUsername());

        return ResponseEntity.status(201)
                .body(postService.createAnnouncement(user.getId(), announcementRequestDto));
    }

    /**
     * 이벤트를 생성하는 API입니다.
     * 
     * @param user            인증된 관리자 정보
     * @param eventRequestDto 이벤트 생성 요청 데이터
     * @return 생성된 이벤트 정보
     */
    @PostMapping("/event")
    public ResponseEntity<PostResponseDto> createEvent(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody EventRequestDto eventRequestDto) {
        log.info("Request to create an event. User: {}", user.getUsername());

        return ResponseEntity.status(201)
                .body(postService.createEvent(user.getId(), eventRequestDto));
    }

    /**
     * 공지사항을 수정하는 API입니다.
     * 
     * @param postId                 수정할 공지사항 ID
     * @param announcementRequestDto 공지사항 수정 요청 데이터
     * @return 수정된 공지사항 정보
     */
    @PutMapping("/announcement/{postId}")
    public ResponseEntity<PostResponseDto> updateAnnouncement(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody AnnouncementRequestDto announcementRequestDto) {
        log.info("Request to update an announcement. Post ID: {}", postId);

        return ResponseEntity.ok(postService.updateAnnouncement(postId, announcementRequestDto));
    }

    /**
     * 이벤트를 수정하는 API입니다.
     * 
     * @param postId          수정할 이벤트 ID
     * @param eventRequestDto 이벤트 수정 요청 데이터
     * @return 수정된 이벤트 정보
     */
    @PutMapping("/event/{postId}")
    public ResponseEntity<PostResponseDto> updateEvent(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody EventRequestDto eventRequestDto) {
        log.info("Request to update an event. Post ID: {}", postId);

        return ResponseEntity.ok(postService.updateEvent(postId, eventRequestDto));
    }

    /**
     * 게시물(공지사항/이벤트)을 삭제하는 API입니다.
     * 
     * @param postId 삭제할 게시물 ID
     * @return 성공 메시지
     */
    @PostMapping("/{postId}/delete")
    public ResponseEntity<String> deletePost(@PathVariable("postId") Long postId) {
        log.info("Request to delete a post. Post ID: {}", postId);

        postService.deletePost(postId);
        return ResponseEntity.ok("게시물 삭제에 성공하였습니다.");
    }
}
