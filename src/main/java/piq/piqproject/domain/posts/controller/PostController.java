package piq.piqproject.domain.posts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.posts.dto.PostRequestDto;
import piq.piqproject.domain.posts.dto.PostResponseDto;
import piq.piqproject.domain.posts.entity.PostType;
import piq.piqproject.domain.posts.service.PostService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/posts")
public class PostController {
    private final PostService postService;

    //@PreAuthorize("hasRole('ROLE_ADMIN')") todo: 권한 추가 후 주석 제거
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam PostType type,
            @Valid @RequestBody PostRequestDto postRequestDto
    ) {
        return ResponseEntity.status(201)
                .body(postService.createPost(user.getId(), type, postRequestDto));
    }
}
