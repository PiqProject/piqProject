package piq.piqproject.domain.posts.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.service.PostService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/posts")
public class PostController {
        private final PostService postService;

        @GetMapping("/{postId}")
        public ResponseEntity<PostResponseDto> findPost(
                        @PathVariable("postId") Long postId) {
                log.info("Request to find a single post. Post ID: {}", postId);

                return ResponseEntity.ok(postService.findPost(postId));
        }

        /**
         * PostEntity의 Type(EVENT,ANNOUNCEMENT)에 상관없이 모든 게시글을 반환함
         * 
         * @param pageable
         * @return
         */
        @GetMapping("/all")
        public ResponseEntity<Page<PostResponseDto>> getPosts(
                        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                log.info("""
                                Request to get all posts.
                                Page number: {},
                                Page size: {},
                                Sort: {}
                                """,
                                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

                return ResponseEntity.ok(postService.getPost(pageable));
        }

}