package piq.piqproject.domain.posts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.posts.dto.request.AnnouncementRequestDto;
import piq.piqproject.domain.posts.dto.request.EventRequestDto;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.service.PostService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/posts")
public class PostController {
        private final PostService postService;

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PostMapping("/announcement")
        public ResponseEntity<PostResponseDto> createAnnouncement(
                @AuthenticationPrincipal UserEntity user,
                @Valid @RequestBody AnnouncementRequestDto announcementRequestDto
        ) {
                log.info("Request to create an announcement. User: {}", user.getUsername());

                return ResponseEntity.status(201)
                                .body(postService.createAnnouncement(user.getId(), announcementRequestDto));
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PostMapping("/event")
        public ResponseEntity<PostResponseDto> createEvent(
                @AuthenticationPrincipal UserEntity user,
                @Valid @RequestBody EventRequestDto eventRequestDto
        ) {
                log.info("Request to create an event. User: {}", user.getUsername());

                return ResponseEntity.status(201)
                                .body(postService.createEvent(user.getId(), eventRequestDto));
        }

        @GetMapping("/{postId}")
        public ResponseEntity<PostResponseDto> findPost(
                @PathVariable("postId") Long postId
        ) {
                log.info("Request to find a single post. Post ID: {}", postId);

                return ResponseEntity.ok(postService.findPost(postId));
        }

        @GetMapping("/all")
        public ResponseEntity<Page<PostResponseDto>> getPost(
                @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
        ) {
                log.info("""
                        Request to get all posts. 
                        Page number: {}, 
                        Page size: {}, 
                        Sort: {}
                        """, 
                        pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()
                );

                return ResponseEntity.ok(postService.getPost(pageable));
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PutMapping("/announcement/{postId}")
        public ResponseEntity<PostResponseDto> updateAnnouncement(
                @PathVariable("postId") Long postId,
                @Valid @RequestBody AnnouncementRequestDto announcementRequestDto
        ) {
                log.info("Request to update an announcement. Post ID: {}", postId);

                return ResponseEntity.ok(postService.updateAnnouncement(postId, announcementRequestDto));
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PutMapping("/event/{postId}")
        public ResponseEntity<PostResponseDto> updateEvent(
                @PathVariable("postId") Long postId,
                @Valid @RequestBody EventRequestDto eventRequestDto
        ) {
                log.info("Request to update an event. Post ID: {}", postId);

                return ResponseEntity.ok(postService.updateEvent(postId, eventRequestDto));
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PostMapping("/{postId}/delete")
        public ResponseEntity<String> deletePost(
                        @PathVariable("postId") Long postId
        ) {
                log.info("Request to delete a post. Post ID: {}", postId);

                postService.deletePost(postId);
                return ResponseEntity.ok("게시물 삭제에 성공하였습니다.");
        }
}