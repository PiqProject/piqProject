package piq.piqproject.domain.users.service;

import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_INTEREST;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.file.FileUploader;
import piq.piqproject.common.file.FileUtil;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.interests.entity.InterestEntity;
import piq.piqproject.domain.interests.repository.InterestRepository;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.traits.entity.TraitOptionEntity;
import piq.piqproject.domain.traits.repository.TraitOptionRepository;
import piq.piqproject.domain.users.dto.request.UserIdealRequestDto;
import piq.piqproject.domain.users.dto.request.UserInterestRequestDto;
import piq.piqproject.domain.users.dto.request.UserScoreRequestDto;
import piq.piqproject.domain.users.dto.request.UserTraitRequestDto;
import piq.piqproject.domain.users.dto.response.UserIdealResponseDto;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.dto.response.UserScoreResponseDto;
import piq.piqproject.domain.users.dto.response.UserTraitResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.entity.UserIdealEntity;
import piq.piqproject.domain.users.entity.UserInterestEntity;
import piq.piqproject.domain.users.entity.UserTraitEntity;
import piq.piqproject.domain.users.repository.UserIdealRepository;
import piq.piqproject.domain.users.repository.UserInterestRepository;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.users.repository.UserTraitRepository;

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
    private final TraitOptionRepository traitOptionRepository;
    private final UserIdealRepository userIdealRepository;
    private final UserTraitRepository userTraitRepository;
    private final MatchingRepository matchingRepository;

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
    public ListResponseDto<UserInterestResponseDto> upsertUserInterests(UserEntity user,
            UserInterestRequestDto userInterestRequestDto) {
        // 요청으로 들어온 관심사 ID들의 유효성을 검증
        List<Long> interestIds = userInterestRequestDto.getInterestIds();
        List<InterestEntity> interests = interestRepository.findAllById(interestIds);

        // 요청된 ID의 수와 실제 조회된 관심사의 수가 다르면 예외 발생
        if (interests.size() != interestIds.size()) {
            throw new NotFoundException(NOT_FOUND_INTEREST);
        }

        List<UserInterestEntity> userInterests = userInterestRepository.findAllByUserId(user.getId());

        // 기존 관심사가 있다면 한 번의 쿼리로 모두 삭제하여 성능을 최적화
        if (!userInterests.isEmpty()) {
            userInterestRepository.deleteAllInBatch(userInterests);
        }

        List<UserInterestEntity> newUserInterestList = interests.stream()
                .map(interest -> UserInterestEntity.of(user, interest))
                .toList();

        userInterestRepository.saveAll(newUserInterestList);

        List<UserInterestResponseDto> userInterestResponse = newUserInterestList.stream()
                .map(UserInterestResponseDto::of)
                .toList();

        return ListResponseDto.from(userInterestResponse);
    }

    @Transactional
    public ListResponseDto<UserIdealResponseDto> upsertUserIdeals(UserEntity user,
            UserIdealRequestDto userIdealRequestDto) {
        // 요청으로 들어온 이상형 옵션 ID들의 유효성을 검증
        List<Long> idealOptionIds = userIdealRequestDto.getIdealOptionIds();
        List<TraitOptionEntity> idealOptions = traitOptionRepository.findAllById(idealOptionIds);

        // 요청된 ID의 수와 실제 조회된 관심사의 수가 다르면 예외 발생
        if (idealOptions.size() != idealOptionIds.size()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_OPTION,
                    "요청된 이상형 옵션 ID와 실제 조회된 이상형 옵션 ID의 수가 일치하지 않습니다.");
        }

        List<UserIdealEntity> userIdeals = userIdealRepository.findAllByUserId(user.getId());

        // 기존 이상형이 있다면 한 번의 쿼리로 모두 삭제하여 성능을 최적화
        if (!userIdeals.isEmpty()) {
            userIdealRepository.deleteAllInBatch(userIdeals);
        }

        List<UserIdealEntity> userIdealList = idealOptions.stream()
                .map(option -> UserIdealEntity.of(user, option))
                .toList();

        userIdealRepository.saveAll(userIdealList);

        List<UserIdealResponseDto> userIdealResponse = userIdealList.stream()
                .map(UserIdealResponseDto::of)
                .toList();

        return ListResponseDto.from(userIdealResponse);
    }

    @Transactional
    public UserScoreResponseDto scoreUser(UserEntity scorerUser, UserScoreRequestDto userScoreRequestDto) {

        Long scorerId = scorerUser.getId();
        Long targetId = userScoreRequestDto.getTargerUserId();

        // 1. 점수를 받을 유저(targetUser)를 조회합니다.
        UserEntity targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "점수를 받을 유저를 찾을 수 없습니다."));

        // 2. 두 유저 간의 매칭 정보를 조회합니다.
        // sender, receiver 순서에 상관없이 매칭을 찾아야 합니다.
        MatchingEntity match = matchingRepository.findMatchBetweenUsers(scorerId, targetId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "두 유저 간의 매칭 정보를 찾을 수 없습니다."));

        // 3. 매칭 상태가 'SUCCESS'가 아니면 예외를 발생시킵니다.
        if (match.getStatus() != MatchingStatus.SUCCESS) {
            throw new InvalidRequestException(ErrorCode.INVALID_MATCH_STATUS);
        }

        // 5. targetUser의 점수를 업데이트합니다. (평균 계산 로직은 UserEntity로 위임)
        int score = userScoreRequestDto.getScore();
        targetUser.updateScore(score);

        // 7. 변경된 유저의 최종 정보를 담아 DTO로 반환합니다.
        return UserScoreResponseDto.of(targetUser.getNickname(), targetUser.getAverageScore());
    }

    /**
     * 사용자의 실제 특성 목록을 생성하거나 전체 수정합니다. (Upsert)
     *
     * @param user                현재 사용자 엔티티
     * @param userTraitRequestDto 사용자가 선택한 특성 옵션 ID 목록을 담은 DTO
     * @return 업데이트된 사용자의 특성 목록 DTO
     */
    @Transactional
    public ListResponseDto<UserTraitResponseDto> upsertUserTraits(UserEntity user,
            UserTraitRequestDto userTraitRequestDto) {

        // 1. 요청으로 들어온 특성 옵션 ID들의 유효성을 검증합니다.
        // 내 특성id들 조회
        List<Long> traitOptionIds = userTraitRequestDto.getTraitOptionIds();
        // 특성 정보들 조회
        List<TraitOptionEntity> traitOptions = traitOptionRepository.findAllById(traitOptionIds);

        // 2. 요청된 ID의 수와 실제 DB에서 조회된 엔티티의 수가 다르면, 유효하지 않은 ID가 포함된 것이므로 예외를 발생시킵니다.
        if (traitOptions.size() != traitOptionIds.size()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND_TRAIT_OPTION,
                    "요청된 특성 옵션 ID 중 일부가 유효하지 않습니다.");
        }

        // 3. 사용자의 기존 특성 목록을 조회합니다.
        List<UserTraitEntity> userTraits = userTraitRepository.findAllByUserId(user.getId());

        // 4. 기존 특성이 있다면, 한 번의 DELETE 쿼리로 모두 삭제하여 성능을 최적화합니다.
        if (!userTraits.isEmpty()) {
            userTraitRepository.deleteAllInBatch(userTraits);
        }

        // 5. 새로운 특성 목록을 생성합니다.
        List<UserTraitEntity> newUserTraitList = traitOptions.stream()
                .map(option -> UserTraitEntity.of(user, option))
                .toList(); // Java 16+

        // 6. 생성된 새 특성 목록을 한 번의 INSERT 쿼리(bulk insert)로 저장합니다.
        userTraitRepository.saveAll(newUserTraitList);

        // 7. 저장된 최종 특성 목록을 클라이언트에게 반환할 응답 DTO로 변환합니다.
        List<UserTraitResponseDto> userTraitResponse = newUserTraitList.stream()
                .map(userTrait -> UserTraitResponseDto.from(userTrait.getTraitOption()))
                .toList();

        return ListResponseDto.from(userTraitResponse);
    }
}