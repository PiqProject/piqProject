package piq.piqproject.domain.posts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.posts.dto.request.AnnouncementRequestDto;
import piq.piqproject.domain.posts.dto.request.EventRequestDto;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.service.PostService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * TODO: 각 메서드에 권한 검증 추가하기 @PreAuthorize("hasRole('ROLE_ADMIN')")
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
}
