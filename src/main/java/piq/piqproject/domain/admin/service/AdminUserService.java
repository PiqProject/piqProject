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
import piq.piqproject.domain.userimages.repository.UserImageRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserImageRepository userImageRepository;
    private final FileUploader fileUploader;

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

        if (amount > 0) {
            user.refundPqPoints(amount); // 지급
        } else if (amount < 0) {
            // 차감 시 보유 포인트보다 많은지 체크 로직
            if (user.getPqPoint() >= Math.abs(amount)) {
                user.deductPqPoints(Math.abs(amount));
            } else {
                user.deductPqPoints(user.getPqPoint());
            }
        }

        // TODO: 포인트 변동 내역(PointHistory) 엔티티가 있다면 여기에 사유(reason)와 함께 저장해야 합니다.
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
}