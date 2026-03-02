package piq.piqproject.domain.admin.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import piq.piqproject.domain.admin.dto.request.BulkPointRequestDto;
import piq.piqproject.domain.admin.dto.request.PointRequestDto;
import piq.piqproject.domain.admin.dto.request.UserStatusRequestDto;
import piq.piqproject.domain.admin.dto.response.UserAdminDetailResponseDto;
import piq.piqproject.domain.admin.dto.response.UserAdminResponseDto;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.notifications.enums.NotificationType;
import piq.piqproject.domain.notifications.service.NotificationService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;
import piq.piqproject.domain.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final FileUploader fileUploader;
    private final PointService pointService;
    private final NotificationService notificationService;

    /**
     * 회원 목록 조회 (검색 지원)
     */
    public Page<UserAdminResponseDto> getUsers(Long id, Pageable pageable) {
        Page<UserEntity> userPage;

        if (id != null) {
            userPage = userRepository.searchAdminUsers(id, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return userPage.map(UserAdminResponseDto::from);
    }

    /**
     * 회원 상세 조회
     */
    public UserAdminDetailResponseDto getUserDetail(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        return UserAdminDetailResponseDto.from(user);
    }

    /**
     * 회원 상태 변경 (활성/비활성)
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatusRequestDto requestDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        user.updateActiveStatus(requestDto.getIsActive());

        // 계정이 비활성화(정지)되는 경우 알림 전송
        if (Boolean.FALSE.equals(requestDto.getIsActive())) {
            notificationService.notify(user, NotificationType.ACCOUNT_DISABLED,
                    "계정 이용이 제한되었습니다.",
                    "가이드라인 위반 및 운영 정책에 따라 계정이 비활성화되었습니다. 1:1 문의를 통해 해제요청을 남겨주시기 바랍니다.",
                    "/inquiry");
        }
    }

    /**
     * 포인트 수동 지급/회수
     */
    @Transactional
    public void adjustPoint(Long userId, PointRequestDto requestDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        int amount = requestDto.getAmount();
        String reason = requestDto.getReason();

        if (amount > 0) {
            pointService.chargePoints(user, amount, PointType.ADMIN, "관리자 수동 지급: " + reason);
        } else if (amount < 0) {
            int absoluteAmount = Math.abs(amount);
            // 차감 시 보유 포인트보다 많은지 체크 로직 (기존 drain 로직 유지)
            int actualDeductAmount = Math.min(user.getPqPoint(), absoluteAmount);
            if (actualDeductAmount > 0) {
                pointService.usePoints(user, actualDeductAmount, "관리자 수동 차감: " + reason);
            }
        }
    }

    /**
     * 전회원 또는 특정 성별 대상 포인트 일괄 지급/회수
     */
    @Transactional
    public void adjustBulkPoint(BulkPointRequestDto requestDto) {
        List<UserEntity> targets;
        if (requestDto.getTargetGender() == Gender.MALE || requestDto.getTargetGender() == Gender.FEMALE) {
            targets = userRepository.findAllByGender(requestDto.getTargetGender());
        } else {
            targets = userRepository.findAll();
        }

        int amount = requestDto.getAmount();
        String reason = "관리자 일괄 " + (amount > 0 ? "지급" : "차감") + ": " + requestDto.getReason();

        pointService.adjustPointsBulk(targets, amount, reason);
    }

    /**
     * 회원 강제 탈퇴
     * - DB 데이터 논리적 삭제 (FK 에러 방지 및 개인정보 파기)
     * - S3에 저장된 프로필 이미지 및 음성 파일 비동기 삭제 (성능 개선)
     */
    @Transactional
    public void deleteUserForcefully(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        // 1. 삭제할 파일 URL 목록 수집 (이미지 + 음성)
        List<String> filesToDelete = user.getImages().stream()
                .map(UserImageEntity::getImageUrl)
                .collect(Collectors.toList());

        if (user.getVoiceUrl() != null) {
            filesToDelete.add(user.getVoiceUrl());
        }

        // [개인정보 즉시 파기 및 상태 변경] 로직을 호출합니다.
        user.executePermanentWithdrawal();

        // 3. 트랜잭션 커밋 후 S3 파일 삭제 (성능 안 좋은 문제 해결!)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // CompletableFuture.runAsync()로 별도의 스레드(백그라운드)에 던져버립니다.
                CompletableFuture.runAsync(() -> {
                    log.info("user s3 File delete try: {} files", filesToDelete.size());
                    for (String url : filesToDelete) {
                        try {
                            fileUploader.delete(url);
                        } catch (Exception e) {
                            // TODO: S3 파일 삭제 실패 저장하고 나중에 다시 시도하는 로직 필요
                            log.error("[S3_DELETE_FAIL] admin force user delete: s3 file error URL: {}", url, e);
                        }
                    }
                });
            }
        });
    }
}