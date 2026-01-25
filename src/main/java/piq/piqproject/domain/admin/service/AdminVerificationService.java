package piq.piqproject.domain.admin.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.domain.admin.dto.response.UserVerificationResponseDto;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.userimages.repository.UserImageRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.verification.entity.VerificationEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.repository.VerificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    private final VerificationRepository verificationRepository;
    private final UserImageRepository userImageRepository;
    private final UserRepository userRepository;
    private final FileUploader fileUploader;

    @Transactional(readOnly = true)
    public Page<UserVerificationResponseDto> getVerifications(Pageable pageable) {
        Page<VerificationEntity> verifications = verificationRepository.findAll(pageable);

        return verifications.map(UserVerificationResponseDto::of);
    }

    /**
     * 부적절한 사진 강제 삭제
     */
    @Transactional
    public void deleteUserImageForcefully(Long userId, Long imageId) {
        // 1. 이미지 조회
        UserImageEntity image = userImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다."));

        // 2. 해당 유저의 이미지가 맞는지 검증
        if (!image.getUser().getId().equals(userId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND, "해당 유저의 이미지가 아닙니다.");
        }

        String imageUrl = image.getImageUrl();

        // 3. DB 삭제
        userImageRepository.delete(image);

        // 4. S3 삭제 (커밋 후)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fileUploader.delete(imageUrl);
                    log.info("관리자에 의한 부적절 사진 삭제 완료. URL: {}", imageUrl);
                } catch (Exception e) {
                    log.error("[S3_DELETE_FAIL] 관리자 사진 삭제 실패. URL: {}", imageUrl, e);
                }
            }
        });
    }

    /**
     * 회원 음성 강제 삭제
     */
    @Transactional
    public void deleteUserVoiceForcefully(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        String voiceUrl = user.getVoiceUrl();

        // 1. 음성 파일이 있는지 확인
        if (voiceUrl == null || voiceUrl.isEmpty()) {
            throw new IllegalArgumentException("삭제할 음성 파일이 없습니다.");
        }

        // 2. DB 업데이트 (URL 제거)
        user.updateVoiceUrl(null);

        // 3. S3 파일 삭제 (트랜잭션 커밋 후 실행)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("관리자에 의한 음성 삭제 완료. S3 파일 삭제 시작. URL: {}", voiceUrl);
                try {
                    fileUploader.delete(voiceUrl);
                } catch (Exception e) {
                    log.error("[S3_DELETE_FAIL] 관리자 음성 삭제 실패. URL: {}", voiceUrl, e);
                }
            }
        });
    }

    /**
     * 프로필(소개글) 강제 초기화
     */
    @Transactional
    public void resetUserIntroduce(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        // 엔티티 비즈니스 메서드 호출 (Dirty Checking으로 자동 update)
        user.resetIntroduce();
    }

    @Transactional
    public void approveVerification(Long verificationId) {
        VerificationEntity verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));
        verification.approve();
    }

    public void rejectVerification(Long verificationId) {
        VerificationEntity verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

        verification.reject();
    }

}
