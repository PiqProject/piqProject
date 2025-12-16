package piq.piqproject.domain.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.search.document.UserDocument;
import piq.piqproject.domain.search.repository.es.UserDocumentRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserRepository userRepository;
    private final UserDocumentRepository userDocumentRepository;

    /**
     * UserEntity와 elasticSearch Document를 동기화
     * 이 메서드는 "성공" 아니면 "에러 발생" 둘 중 하나
     */
    @Transactional(readOnly = true)
    public void syncUserToElasticsearch(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        UserDocument document = UserDocument.from(user);
        userDocumentRepository.save(document);

        log.info("ES 동기화 성공: UserId {}", userId);
    }
}