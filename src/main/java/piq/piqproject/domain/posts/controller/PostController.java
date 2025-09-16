package piq.piqproject.domain.posts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import piq.piqproject.domain.posts.dto.request.CreateAnnouncementRequestDto;
import piq.piqproject.domain.posts.dto.request.CreateEventRequestDto;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.service.PostService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/posts")
public class PostController {
    private final PostService postService;

    //@PreAuthorize("hasRole('ROLE_ADMIN')") todo: 권한 추가 후 주석 제거
    @PostMapping("/announcement")
    public ResponseEntity<PostResponseDto> createAnnouncement(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody CreateAnnouncementRequestDto createAnnouncementRequestDto
    ) {
        return ResponseEntity.status(201)
                .body(postService.createAnnouncement(user.getId(), createAnnouncementRequestDto));
    }

    //@PreAuthorize("hasRole('ROLE_ADMIN')") todo: 권한 추가 후 주석 제거
    @PostMapping("/event")
    public ResponseEntity<PostResponseDto> createEvent(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody CreateEventRequestDto createEventRequestDto
    ) {
        return ResponseEntity.status(201)
                .body(postService.createEvent(user.getId(), createEventRequestDto));
    }
}
