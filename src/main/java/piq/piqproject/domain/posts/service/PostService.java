package piq.piqproject.domain.posts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ErrorCode;
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

        postDao.savePost(post);

        return PostResponseDto.of(
                post.getTitle(),
                post.getContent(),
                post.getStartDate().format(DateTimeFormatter.ISO_DATE_TIME),
                post.getEndDate().format(DateTimeFormatter.ISO_DATE_TIME),
                post.getCreatedAt().format(DateTimeFormatter.ISO_DATE)
        );
    }
}
