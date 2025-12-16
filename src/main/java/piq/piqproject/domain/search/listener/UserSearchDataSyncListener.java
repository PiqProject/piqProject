package piq.piqproject.domain.search.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.search.entity.SyncFailLogEntity;
import piq.piqproject.domain.search.repository.SyncFailLogRepository;
import piq.piqproject.domain.search.service.UserSearchService;
import piq.piqproject.domain.users.event.UserProfileUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchDataSyncListener {

    private final UserSearchService userSearchService; // 분리한 서비스 주입
    private final SyncFailLogRepository syncFailLogRepository;

    /**
     * 프로필 변경 이벤트가 발생하면 실행됩니다.
     * 
     * @Async: 별도의 스레드에서 비동기로 실행됩니다. (메인 로직에 영향 X)
     *         @TransactionalEventListener(AFTER_COMMIT): RDB 트랜잭션이 완전히 커밋된 '후'에
     *         실행됩니다.
     *         (매우 중요: 커밋 전에 실행되면 변경 전의 데이터를 읽어갈 수 있음)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserProfileUpdatedEvent(UserProfileUpdatedEvent event) {
        Long userId = event.getUserId();
        try {
            userSearchService.syncUserToElasticsearch(userId);

        } catch (Exception e) {
            log.error("ES 동기화 실패 (실패 로그 저장됨) - UserId: {}", userId, e);

            // 실패 시 DB에 저장 (추후 스케줄러가 처리)
            saveFailLog(userId, e.getMessage());
        }
    }

    private void saveFailLog(Long userId, String errorMessage) {
        SyncFailLogEntity failLog = SyncFailLogEntity.builder()
                .userId(userId)
                .errorReason(errorMessage)
                .build();
        syncFailLogRepository.save(failLog);
    }
}