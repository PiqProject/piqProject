package piq.piqproject.domain.voice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.common.file.FileUtil;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.verification.entity.VerificationEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.enums.VerificationStatus;
import piq.piqproject.domain.verification.repository.VerificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final UserRepository userRepository;
    private final FileUploader fileUploader;
    private final FileUtil fileUtil;
    private final VerificationRepository verificationRepository;

    /**
     * 음성 파일 업로드 및 교체
     */
    @Transactional
    public void uploadVoice(UserEntity principalUser, MultipartFile voiceFile) {
        // 1. 파일 유효성 검사 (오디오 파일인지 확인)
        if (!fileUtil.isAudioFile(voiceFile)) {
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "음성 파일(audio/*)만 업로드할 수 있습니다.");
        }

        // 2. 영속성 컨텍스트 유지를 위해 User 조회
        UserEntity user = userRepository.findById(principalUser.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        // 3. 기존 파일 URL 백업 (성공 시 삭제하기 위함)
        String oldVoiceUrl = user.getVoiceUrl();

        // 4. 업로드 경로 생성 및 S3 업로드
        String dirPath = fileUtil.createDirectoryPath("voice");
        String fileName = fileUtil.createUniqueFileName(voiceFile.getOriginalFilename());
        String fullPath = dirPath + "/" + fileName;

        // S3에 파일 업로드 (네트워크 통신 발생)
        String newVoiceUrl = fileUploader.upload(voiceFile, fullPath);

        // 음성 검증 준비
        VerificationEntity verification = VerificationEntity.of(user, ContentType.VOICE, newVoiceUrl,
                VerificationStatus.PENDING);
        verificationRepository.save(verification);

        // TODO: 관리자에게 알림 보내기

        try {
            // 5. DB 업데이트 (Dirty Checking)
            user.updateVoiceUrl(newVoiceUrl);

        } catch (Exception e) {
            // 6. [보상 트랜잭션] DB 저장 실패 시, 방금 S3에 올린 파일을 즉시 삭제해야 함
            // (DB에는 없는데 S3에만 파일이 남는 '고아 파일' 방지)
            log.error("DB 업데이트 실패. 업로드된 파일 롤백을 시도합니다. URL: {}", newVoiceUrl, e);
            fileUploader.delete(newVoiceUrl);
            throw e; // 예외를 다시 던져서 @Transactional 롤백 유도
        }

        // 7. 모든 과정이 성공했다면 기존 파일 삭제 (스토리지 낭비 방지)
        if (oldVoiceUrl != null && !oldVoiceUrl.isEmpty()) {
            fileUploader.delete(oldVoiceUrl);
        }
    }

    /**
     * 음성 파일 삭제
     */
    @Transactional
    public void deleteVoice(UserEntity principalUser) {
        UserEntity user = userRepository.findById(principalUser.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        String voiceUrl = user.getVoiceUrl();
        if (voiceUrl == null || voiceUrl.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND, "삭제할 음성 파일이 없습니다.");
        }

        // 1. DB 업데이트 (URL 제거)
        user.updateVoiceUrl(null);

        // 2. 실제 S3 파일 삭제는 '트랜잭션이 커밋된 후'에 실행 (안전장치)
        // 만약 DB 업데이트가 실패해서 롤백되면, 파일은 삭제하지 않아야 함 (데이터 복구를 위해)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fileUploader.delete(voiceUrl);
                } catch (Exception e) {
                    // 삭제 실패는 로그만 남김 (이미 DB에서는 지워졌으므로 서비스 흐름엔 영향 X)
                    // TODO: 이런 로그를 모아놓은 Table을 만들고 scheduler를 통해 특정시점마다 삭제하는 로직 구현할 것
                    log.error("S3 파일 삭제 중 오류 발생. DB에선 지워짐 URL: {}", voiceUrl, e);
                }
            }
        });
    }
}