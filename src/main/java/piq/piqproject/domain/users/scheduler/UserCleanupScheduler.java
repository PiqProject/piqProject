package piq.piqproject.domain.users.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final FileUploader fileUploader;

    /**
     * 매일 새벽 3시에 7일이 지난 탈퇴 계정을 영구 삭제합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupWithdrawnUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<UserEntity> expiredUsers = userRepository.findByWithdrawnAtBefore(threshold);

        if (expiredUsers.isEmpty()) {
            return;
        }

        log.info("Starting cleanup for {} expired withdrawal accounts", expiredUsers.size());

        for (UserEntity user : expiredUsers) {
            try {
                // 1. S3 파일 삭제 목록 수집
                List<String> filesToDelete = user.getImages().stream()
                        .map(UserImageEntity::getImageUrl)
                        .collect(Collectors.toList());

                if (user.getVoiceUrl() != null) {
                    filesToDelete.add(user.getVoiceUrl());
                }

                // 2. DB에서 개인정보 파기
                user.executePermanentWithdrawal();

                // 3. S3 파일 삭제 실행
                for (String url : filesToDelete) {
                    try {
                        fileUploader.delete(url);
                    } catch (Exception e) {
                        log.error("[S3_CLEANUP_FAIL] Failed to delete file during user cleanup. URL: {}", url, e);
                    }
                }
                log.info("Permanently deleted user: {}", user.getId());
            } catch (Exception e) {
                log.error("[USER_CLEANUP_FAIL] Failed to permanently delete user: {}", user.getId(), e);
            }

        }

        log.info("Cleanup process completed.");
    }
}