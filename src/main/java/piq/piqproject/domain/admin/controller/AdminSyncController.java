package piq.piqproject.domain.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import piq.piqproject.domain.search.document.UserDocument;
import piq.piqproject.domain.search.repository.es.UserDocumentRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final UserRepository userRepository;
    private final UserDocumentRepository userDocumentRepository;

    /**
     * [관리자용] RDB의 모든 유저 데이터를 Elasticsearch로 전체 동기화 (Full Refresh)
     * TODO: 데이터가 많을 경우 Paging 처리(Batch)가 필요하지만, 초기 단계에는 findAll로 구현.
     */
    @PostMapping("/users")
    @Transactional(readOnly = true) // 지연 로딩을 위해 트랜잭션 유지
    public ResponseEntity<String> syncAllUsers() {
        log.info("전체 유저 데이터 동기화 시작...");

        // 1. 모든 유저 조회
        List<UserEntity> allUsers = userRepository.findAll();

        if (allUsers.isEmpty()) {
            return ResponseEntity.ok("동기화할 유저가 없습니다.");
        }

        // 2. Document로 변환
        List<UserDocument> documents = allUsers.stream()
                .map(UserDocument::from)
                .collect(Collectors.toList());

        // 3. Elasticsearch에 일괄 저장
        userDocumentRepository.saveAll(documents);

        log.info("총 {} 명의 유저 데이터 동기화 완료.", documents.size());
        return ResponseEntity.ok("동기화 완료: " + documents.size() + "명");
    }
}