package piq.piqproject.domain.posts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.posts.dao.PostDao;
import piq.piqproject.domain.posts.dto.PostRequestDto;
import piq.piqproject.domain.posts.dto.PostResponseDto;
import piq.piqproject.domain.posts.entity.PostEntity;
import piq.piqproject.domain.posts.entity.PostType;
import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.entity.UserEntity;

import java.time.format.DateTimeFormatter;

import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_USER;

@Service
@RequiredArgsConstructor
public class PostService {

    private final UserDao userDao;
    private final PostDao postDao;

    @Transactional
    public PostResponseDto createPost(Long userId, PostType type, PostRequestDto postRequestDto) {

        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        PostEntity post = PostEntity.of(user, type, postRequestDto);

        //todo: startDate, endDate 검증 로직 추가

        PostEntity savedPost = postDao.savePost(post);

        return PostResponseDto.of(
                savedPost.getId(),
                savedPost.getTitle(),
                savedPost.getContent(),
                savedPost.getType(),
                savedPost.getStartDate() != null ? post.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                savedPost.getEndDate() != null ? post.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                savedPost.getCreatedAt().format(DateTimeFormatter.ISO_DATE)
        );
    }
}
