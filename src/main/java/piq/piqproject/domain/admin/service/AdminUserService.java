package piq.piqproject.domain.admin.service;

import java.util.List;
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
import piq.piqproject.domain.admin.dto.request.PointRequestDto;
import piq.piqproject.domain.admin.dto.request.UserStatusRequestDto;
import piq.piqproject.domain.admin.dto.response.UserAdminDetailResponseDto;
import piq.piqproject.domain.admin.dto.response.UserAdminResponseDto;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final FileUploader fileUploader;
    private final PointService pointService;

    /**
     * 회원 목록 조회 (검색 지원)
     */
    public Page<UserAdminResponseDto> getUsers(String keyword, Pageable pageable) {
        Page<UserEntity> userPage;

        if (keyword != null && !keyword.isBlank()) {
            userPage = userRepository.findByNicknameContainingOrEmailContaining(keyword, keyword, pageable);
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
     * 회원 강제 탈퇴
     * - DB 데이터 삭제 (Cascade 설정에 의해 연관 데이터 삭제)
     * - S3에 저장된 프로필 이미지 및 음성 파일 삭제
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

        // 2. DB 삭제 (CascadeType.ALL에 의해 연관 엔티티들도 삭제됨)
        userRepository.delete(user);

        // 3. 트랜잭션 커밋 후 S3 파일 삭제 (안전장치)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("관리자에 의한 회원 삭제 완료. S3 파일 정리 시작. 대상 {}개", filesToDelete.size());
                for (String url : filesToDelete) {
                    try {
                        fileUploader.delete(url);
                    } catch (Exception e) {
                        // TODO: S3 파일 삭제 실패 저장하고 나중에 다시 시도하는 로직 필요
                        log.error("[S3_DELETE_FAIL] 관리자 강제 탈퇴 중 파일 삭제 실패. URL: {}", url, e);
                    }
                }
            }
        });
    }
}