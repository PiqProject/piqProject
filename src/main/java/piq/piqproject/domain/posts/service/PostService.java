package piq.piqproject.domain.posts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.posts.dto.request.AnnouncementRequestDto;
import piq.piqproject.domain.posts.dto.request.EventRequestDto;
import piq.piqproject.domain.posts.dto.response.PostResponseDto;
import piq.piqproject.domain.posts.entity.PostEntity;
import piq.piqproject.domain.posts.entity.PostType;
import piq.piqproject.domain.posts.repository.PostRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import static piq.piqproject.common.error.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public PostResponseDto createAnnouncement(Long userId, AnnouncementRequestDto announcementRequestDto) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        // [Request 처리: DTO → Entity]
        // 도메인 계층(Entity)의 순수성을 지키기 위해 표현 계층의 객체(DTO)를 직접 전달하지 않습니다.
        // 서비스 계층에서 필요한 데이터를 추출하여 순수한 값으로 Entity 생성을 위임합니다.
        // 이를 통해 Entity는 DTO의 존재를 전혀 알지 못하게 되어 계층 간 결합도가 낮아집니다.
        PostEntity post = PostEntity.createAnnouncement(
                user,
                announcementRequestDto.getTitle(),
                announcementRequestDto.getContent()
        );

        postRepository.save(post);

        // [Response 처리: Entity → DTO]
        // 표현 계층(DTO)이 도메인 계층(Entity)을 아는 것은 올바른 의존성 방향입니다.
        // Entity 객체를 통째로 넘겨 데이터 변환 및 가공의 책임을 DTO에 완전히 위임합니다.
        return PostResponseDto.of(post);
    }

    @Transactional
    public PostResponseDto createEvent(Long userId, EventRequestDto eventRequestDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        PostEntity post = PostEntity.createEvent(
                user,
                eventRequestDto.getTitle(),
                eventRequestDto.getContent(),
                eventRequestDto.getStartDate(),
                eventRequestDto.getEndDate()
        );

        postRepository.save(post);
        return PostResponseDto.of(post);
    }

    @Transactional
    public PostResponseDto updateAnnouncement(Long postId, AnnouncementRequestDto announcementRequestDto) {

        //TODO: 공지사항 에러 메세지와 이벤트 에러메세지 다르게 설정하기
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_POST));

        // 게시글 유형이 '공지사항'인지 확인
        if (!post.getType().equals(PostType.ANNOUNCEMENT)) {
            throw new InvalidRequestException(POST_TYPE_MISMATCH);
        }

        post.updateAnnouncement(announcementRequestDto.getTitle(), announcementRequestDto.getContent());

        return PostResponseDto.of(post);
    }

    @Transactional
    public PostResponseDto updateEvent(Long postId, EventRequestDto eventRequestDto) {
        //TODO: 공지사항 에러 메세지와 이벤트 에러메세지 다르게 설정하기
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_POST));

        // 게시글 유형이 '이벤트'인지 확인
        if (!post.getType().equals(PostType.EVENT)) {
            throw new InvalidRequestException(POST_TYPE_MISMATCH);
        }

        post.updateEvent(
                eventRequestDto.getTitle(),
                eventRequestDto.getContent(),
                eventRequestDto.getStartDate(),
                eventRequestDto.getEndDate()
        );

        return PostResponseDto.of(post);
    }
}
