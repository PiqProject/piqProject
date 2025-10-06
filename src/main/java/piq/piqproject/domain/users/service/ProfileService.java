package piq.piqproject.domain.users.service;

import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_INTEREST;
import static piq.piqproject.common.error.exception.ErrorCode.USER_INTERESTS_ALREADY_REGISTERED;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.common.file.FileUtil;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.interests.entity.InterestEntity;
import piq.piqproject.domain.interests.repository.InterestRepository;
import piq.piqproject.domain.users.dto.request.UserInterestRequestDto;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.entity.UserInterestEntity;
import piq.piqproject.domain.users.repository.UserInterestRepository;
import piq.piqproject.domain.users.repository.UserRepository;

/**
 * ProfileService는 사용자 프로필수정 비지니스로직을 담당합니다.
 * 
 * 주요 기능:
 * - 사용자 프로필 수정 (사진은 UserImageService에서 담당(분리))
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final FileUploader fileUploader;
    private final FileUtil fileUtil;
    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;

    /**
     * 사용자의 프로필 음성을 업로드(또는 교체)하는 메서드
     * 
     * @param userId    현재 로그인한 사용자의 ID
     * @param voiceFile 업로드할 음성 파일
     */
    @Transactional
    public void uploadVoice(Long userId, MultipartFile voiceFile) {
        // 파일 유효성 검사
        if (!fileUtil.isAudioFile(voiceFile)) {
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "음성 파일(audio/*)만 업로드할 수 있습니다.");
        }

        // 0. 사용자 엔티티 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "User not found: " + userId));

        // 1. 기존 파일 URL을 미리 확보
        String oldVoiceUrl = user.getVoiceUrl();

        // 2. 새 파일을 저장할 경로와 이름 생성
        String dirPath = fileUtil.createDirectoryPath("voice");
        String fileName = fileUtil.createUniqueFileName(voiceFile.getOriginalFilename());
        String fullPath = dirPath + "/" + fileName;

        // 3. 새 파일 업로드
        String newVoiceUrl = fileUploader.upload(voiceFile, fullPath);

        try {
            // 4. [DB 작업] 엔티티의 voiceUrl 필드를 새 URL로 업데이트
            user.updateVoiceUrl(newVoiceUrl);

            // (이 메서드는 @Transactional이므로, 메서드가 성공적으로 끝나야 DB에 커밋됨)

        } catch (Exception e) {
            // 5. [보상 트랜잭션] DB 작업 실패 시, 방금 업로드한 새 파일을 즉시 삭제
            log.warn("DB 업데이트 실패. 업로드된 파일 롤백을 시도합니다. URL: {}", newVoiceUrl, e);
            fileUploader.delete(newVoiceUrl); // 보상(취소) 로직

            // 반드시 원래 예외를 다시 던져서 @Transactional이 롤백을 수행하도록 해야 함
            throw e;
        }

        // 6. 모든 DB 작업이 성공적으로 트랜잭션에 포함된 후, 기존 파일을 삭제
        if (oldVoiceUrl != null && !oldVoiceUrl.isEmpty()) {
            fileUploader.delete(oldVoiceUrl);
        }
    }

    /**
     * 현재 사용자의 프로필 음성을 삭제합니다.
     * 
     * @param userId 현재 로그인한 사용자의 ID
     */
    @Transactional
    public void deleteVoice(Long userId) {
        // 1. 사용자 엔티티 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER));

        String voiceUrl = user.getVoiceUrl();
        if (voiceUrl == null || voiceUrl.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND, "No voice file to delete for user: " + userId);
        }

        // 2. DB의 URL을 먼저 null로 업데이트 (아직 커밋 전)
        user.updateVoiceUrl(null);

        // 3. 트랜잭션이 성공적으로 '커밋된 후에' 파일 삭제를 실행하도록 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("DB 커밋 완료. 음성 파일 삭제를 시작합니다. URL: {}", voiceUrl);
                try {
                    fileUploader.delete(voiceUrl);
                } catch (Exception e) {
                    log.error("DB 커밋 후 음성 파일 삭제 실패. URL: {}", voiceUrl, e);
                }
            }
        });
    }

    @Transactional
    public ListResponseDto<UserInterestResponseDto> registerUserInterests(UserEntity user, UserInterestRequestDto userInterestRequestDto) {

        if (userInterestRepository.existsByUser(user)) {
            throw new ConflictException(USER_INTERESTS_ALREADY_REGISTERED);
        }

        List<Long> interestIds = userInterestRequestDto.getInterestIds();
        List<InterestEntity> interests = interestRepository.findAllById(interestIds);

        if (interests.size() != interestIds.size()) {
            throw new NotFoundException(NOT_FOUND_INTEREST);
        }

        List<UserInterestEntity> userInterestList = interests.stream()
                    .map(interest -> UserInterestEntity.of(user, interest))
                    .toList();

        userInterestRepository.saveAll(userInterestList);

        List<UserInterestResponseDto> userInterestResponse = userInterestList.stream()
                        .map(UserInterestResponseDto::of)
                        .toList();

        return ListResponseDto.from(userInterestResponse);
    }
}