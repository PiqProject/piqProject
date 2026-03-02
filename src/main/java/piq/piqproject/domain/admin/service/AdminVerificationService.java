package piq.piqproject.domain.admin.service;

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
import piq.piqproject.domain.admin.dto.request.VerificationSearchRequestDto;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.userimages.repository.UserImageRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.verification.entity.VerificationEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.repository.VerificationRepository;
import piq.piqproject.domain.userimages.service.UserImageService;
import piq.piqproject.domain.notifications.enums.NotificationType;
import piq.piqproject.domain.notifications.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    private final VerificationRepository verificationRepository;
    private final UserImageRepository userImageRepository;
    private final UserRepository userRepository;
    private final FileUploader fileUploader;
    private final UserImageService userImageService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<UserVerificationResponseDto> getVerifications(VerificationSearchRequestDto requestDto,
            Pageable pageable) {
        Page<VerificationEntity> verifications;

        if (requestDto.getContentType() != null && requestDto.getStatus() != null) {
            verifications = verificationRepository.findAllByContentTypeAndStatus(requestDto.getContentType(),
                    requestDto.getStatus(), pageable);
        } else if (requestDto.getContentType() != null) {
            verifications = verificationRepository.findAllByContentType(requestDto.getContentType(), pageable);
        } else if (requestDto.getStatus() != null) {
            verifications = verificationRepository.findAllByStatus(requestDto.getStatus(), pageable);
        } else {
            verifications = verificationRepository.findAll(pageable);
        }

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
                    log.info("Admin deleted inappropriate image. URL: {}", imageUrl);
                } catch (Exception e) {
                    log.error("[S3_DELETE_FAIL] Admin failed to delete image. URL: {}", imageUrl, e);
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
                log.info("Admin deleted voice record. Starting S3 file deletion. URL: {}", voiceUrl);
                try {
                    fileUploader.delete(voiceUrl);
                } catch (Exception e) {
                    log.error("[S3_DELETE_FAIL] Admin failed to delete voice record. URL: {}", voiceUrl, e);
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

        UserEntity user = verification.getUser();
        String contentValue = verification.getContentValue();

        // 검증이 완료되면 db에 이미지 저장
        if (verification.getContentType() == ContentType.INTRODUCE) {
            user.updateIntroduce(contentValue);
        }

        try {
            if (verification.getContentType() == ContentType.IMAGE) {
                userImageService.saveImageToDb(user, contentValue, verification.getIsMainImage());
            }

            if (verification.getContentType() == ContentType.VOICE) {

                // 기존 파일 URL 백업 (성공 시 삭제하기 위함)
                String oldVoiceUrl = user.getVoiceUrl();
                user.updateVoiceUrl(contentValue);

                // 모든 과정이 성공했다면 기존 파일 삭제
                if (oldVoiceUrl != null && !oldVoiceUrl.isEmpty()) {
                    fileUploader.delete(oldVoiceUrl);
                }
            }

            // 승인 알림 전송
            String contentTypeStr = getContentTypeString(verification.getContentType());
            notificationService.notify(user, NotificationType.CONTENT_APPROVED,
                    contentTypeStr + " 승인 완료", "요청하신 " + contentTypeStr + "이(가) 승인되어 프로필에 반영되었습니다.",
                    "/profile");

        } catch (Exception e) {
            // 5. DB 저장 실패 시 방금 올린 S3 파일 삭제
            log.error("DB update failed. Attempting to rollback uploaded file. URL: {}", verification.getContentValue(),
                    e);
            fileUploader.delete(verification.getContentValue());

            // 예외를 다시 던져서 컨트롤러에게 알림
            throw e;
        }
    }

    public void rejectVerification(Long verificationId) {
        VerificationEntity verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "인증 정보를 찾을 수 없습니다."));

        verification.reject();

        // 거절 알림 전송
        String contentTypeStr = getContentTypeString(verification.getContentType());
        notificationService.notify(verification.getUser(), NotificationType.CONTENT_REJECTED,
                contentTypeStr + " 승인 거절", "요청하신 " + contentTypeStr + "이(가) 가이드라인에 부합하지 않아 거절되었습니다.",
                "/profile");
    }

    private String getContentTypeString(ContentType type) {
        return switch (type) {
            case IMAGE -> "사진";
            case VOICE -> "음성";
            case INTRODUCE -> "자기소개";
            default -> "콘텐츠";
        };
    }

}
