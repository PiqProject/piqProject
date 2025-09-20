package piq.piqproject.domain.posts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.posts.dto.request.AnnouncementRequestDto;
import piq.piqproject.domain.posts.dto.request.EventRequestDto;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.service.PostService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * TODO: 조회를 제외한 각 메서드에 권한 검증 추가하기 @PreAuthorize("hasRole('ROLE_ADMIN')")
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/posts")
public class PostController {
    private final PostService postService;

    @PostMapping("/announcement")
    public ResponseEntity<PostResponseDto> createAnnouncement(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody AnnouncementRequestDto announcementRequestDto
    ) {
        return ResponseEntity.status(201)
                .body(postService.createAnnouncement(user.getId(), announcementRequestDto));
    }

    @PostMapping("/event")
    public ResponseEntity<PostResponseDto> createEvent(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody EventRequestDto eventRequestDto
    ) {
        return ResponseEntity.status(201)
                .body(postService.createEvent(user.getId(), eventRequestDto));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> findPost(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(postService.findPost(postId));
    }

    @GetMapping("/all")
    public ResponseEntity<Page<PostResponseDto>> getPost(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getPost(pageable));
    }

    @PutMapping("/announcement/{postId}")
    public ResponseEntity<PostResponseDto> updateAnnouncement(
            @PathVariable Long postId,
            @Valid @RequestBody AnnouncementRequestDto announcementRequestDto
    ) {
        return ResponseEntity.ok(postService.updateAnnouncement(postId, announcementRequestDto));
    }

    @PutMapping("/event/{postId}")
    public ResponseEntity<PostResponseDto> updateEvent(
            @PathVariable Long postId,
            @Valid @RequestBody EventRequestDto eventRequestDto
    ) {
        return ResponseEntity.ok(postService.updateEvent(postId, eventRequestDto));
    }

    @PostMapping("/{postId}/delete")
    public ResponseEntity<String> deletePost(
            @PathVariable Long postId
    ) {
        postService.deletePost(postId);
        return ResponseEntity.ok("게시물 삭제에 성공하였습니다.");
    }
}
