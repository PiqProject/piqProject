package piq.piqproject.infra.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import piq.piqproject.common.file.FileUploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogBackupService {

    private final FileUploader fileUploader;

    @Value("${app.log.root-path}")
    private String LOG_ROOT_PATH;

    /**
     * 어제 날짜의 로그(Info, Error)를 압축하여 S3에 업로드
     */
    public void backupYesterdayLogs() {
        // 어제 날짜 구하기
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        log.info("[LogBackup] {} 날짜의 로그 백업을 시작합니다.", dateStr);

        // 1. Info 로그 처리 (폴더 구조: {LOG_ROOT_PATH}/history/info/info-날짜.log)
        processLogFile("info", "info-" + dateStr + ".log");

        // 2. Error 로그 처리 (폴더 구조: {LOG_ROOT_PATH}/history/error/error-날짜.log)
        processLogFile("error", "error-" + dateStr + ".log");
    }

    private void processLogFile(String type, String fileName) {
        // 원본 파일 경로 찾기
        File sourceFile = Paths.get(LOG_ROOT_PATH, type, fileName).toFile();

        // 파일이 없으면 스킵 (로그가 하나도 안 찍힌 날일 수 있음)
        if (!sourceFile.exists()) {
            log.info("[LogBackup] 백업할 파일이 없습니다: {}", sourceFile.getAbsolutePath());
            return;
        }

        // 압축 파일 임시 생성 (예: info-2025-12-30.log.gz)
        File compressedFile = new File(sourceFile.getAbsolutePath() + ".gz");

        try {
            // 1. 압축 (Gzip)
            compressGzip(sourceFile, compressedFile);

            // 2. S3 업로드
            // S3 저장 경로: logs/2025/12/30/info.log.gz (날짜별 폴더링)
            String s3Key = generateS3Key(type);
            fileUploader.upload(compressedFile, s3Key);

            log.info("[LogBackup] 업로드 성공: {} -> S3: {}", fileName, s3Key);

        } catch (Exception e) {
            log.error("[LogBackup] 백업 실패: {}", fileName, e);
        } finally {
            // 3. 임시 압축 파일 삭제 (원본 로그는 남겨둠)
            if (compressedFile.exists()) {
                compressedFile.delete();
            }
        }
    }

    // Gzip 압축 로직
    private void compressGzip(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
                FileOutputStream fos = new FileOutputStream(target);
                GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
        }
    }

    /**
     * S3 키 생성
     * 
     * @param type
     * @return logs/yyyy/MM/dd/type.log.gz
     */
    private String generateS3Key(String type) {
        // 결과:
        // logs/2025/12/30/info.log.gz
        // logs/2025/12/30/error.log.gz
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String datePath = yesterday.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        return "logs/" + datePath + "/" + type + ".log.gz";
    }
}