package piq.piqproject.infra.storage.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import piq.piqproject.infra.storage.service.LogBackupService;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogBackupScheduler {

    private final LogBackupService logBackupService;

    // 매일 새벽 4시 30분에 실행
    @Scheduled(cron = "0 30 4 * * *")
    public void scheduleLogBackup() {
        // 어제 로그파일을 history에 등록하고 오늘의 로그를 찍도록 강제하기 위해 필수적
        log.info("[LogBackup] 어제 날짜의 로그 백업을 시작합니다.");
        logBackupService.backupYesterdayLogs();
    }
}