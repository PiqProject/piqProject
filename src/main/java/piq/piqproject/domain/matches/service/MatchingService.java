package piq.piqproject.domain.matches.service;

import piq.piqproject.domain.dislikes.entity.DislikeEntity;
import piq.piqproject.domain.dislikes.repository.DislikeRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.ForbiddenException;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.matches.dto.request.MatchingRequestDto;
import piq.piqproject.domain.matches.dto.request.UpdateMatchingRequestDto;
import piq.piqproject.domain.matches.dto.response.ContactExchangeResponseDto;
import piq.piqproject.domain.matches.dto.response.MatchingResponseDto;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.recommendations.repository.DailyRecommendationRepository;
import piq.piqproject.domain.recommendations.service.DailyRecommendationService;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.notifications.enums.NotificationType;
import piq.piqproject.domain.notifications.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingService {

    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository; // 유저 정보를 가져오기 위해 필요
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final DailyRecommendationService dailyRecommendationService;
    private final PointService pointService;
    private final DislikeRepository dislikeRepository;
    private final NotificationService notificationService;

    @Value("${matching.cost.women}")
    private int matchCostForWomen;

    @Value("${matching.cost.men}")
    private int matchCostForMen;

    /**
     * 매칭 요청 생성
     * 
     * @param requestDto (senderId, receiverId)
     * @return 생성된 매칭 정보
     */
    @Transactional
    public MatchingResponseDto createMatch(Long currentId, MatchingRequestDto requestDto) {

        // 1. 요청을 보낸 유저(Sender)와 받는 유저(Receiver) 조회
        UserEntity sender = userRepository.findById(currentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "존재하지 않는 유저입니다."));

        UserEntity receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "존재하지 않는 유저입니다."));

        // 2. 자기 자신에게 매칭을 요청하는 경우 예외 처리
        if (sender.getId().equals(receiver.getId())) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "자기 자신에게 매칭을 요청할 수 없습니다.");
        }

        // 3. 이미 동일한 매칭 요청이 있는지 확인 (A -> B)
        matchingRepository.findBySenderIdAndReceiverId(sender.getId(), receiver.getId())
                .ifPresent(m -> {
                    throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 매칭을 요청한 상대입니다.");
                });

        // 3-1. 추천 기록이 있는지 확인
        List<LocalDateTime> timeRange = dailyRecommendationService.getRecommendationTimeRange();
        LocalDateTime start = timeRange.get(0);
        LocalDateTime end = timeRange.get(1);

        // existsByUser... 로 호출
        if (!dailyRecommendationRepository.existsByUserAndRecommendedUserAndCreatedAtBetween(sender, receiver, start,
                end)) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "'오늘' 추천된 사용자에게만 매칭을 요청할 수 있습니다.");
        }

        // 4. & 5. 성별에 따른 포인트 차감 (PointService에서 잔액 체크 포함)
        int matchCost = getMatchCostByGender(sender.getGender());
        pointService.usePoints(sender, matchCost, "매칭 요청 포인트 차감");

        // 6. 매칭 엔티티 생성 및 저장 (초기 상태는 PENDING)
        int receiverCost = getMatchCostByGender(receiver.getGender());
        MatchingEntity newMatch = MatchingEntity.builder()
                .sender(sender)
                .receiver(receiver)
                .status(MatchingStatus.PENDING)
                .senderUsedPoints(matchCost)
                .receiverUsedPoints(receiverCost)
                .message(requestDto.getMessage())
                .build();

        MatchingEntity savedMatch = matchingRepository.save(newMatch);

        // 7. 오늘의 추천 기록을 조회 하여 '액션 완료' 상태로 업데이트
        dailyRecommendationService.markRecommendationAsActioned(sender, receiver);

        // 알림 전송 (새로운 매칭 도착)
        notificationService.notify(receiver, NotificationType.MATCH_RECEIVED,
                "새로운 매칭 도착", sender.getNickname() + "님으로부터 매칭 요청이 도착했습니다!",
                "/matches/received");

        // 8. DTO로 변환하여 반환
        return MatchingResponseDto.from(savedMatch, currentId);
    }

    /**
     * 매칭 요청 수락 또는 거절
     * 
     * @param matchId    상태를 변경할 매칭의 ID
     * @param currentId  현재 로그인된 사용자(요청을 받은 사람)의 ID
     * @param requestDto 수락/거절 정보가 담긴 DTO
     * @return 업데이트된 매칭 정보
     */
    @Transactional
    public MatchingResponseDto updateMatchStatus(Long matchId, Long currentId,
            UpdateMatchingRequestDto requestDto) {

        // 1. 매칭 정보 조회
        MatchingEntity matching = matchingRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "존재하지 않는 매칭 정보입니다."));

        // 2. 권한 검증: 현재 로그인한 사용자가 매칭을 받은 사람(receiver)이 맞는지 확인
        if (!matching.getReceiver().getId().equals(currentId)) {
            throw new ForbiddenException(ErrorCode.NOT_OWNER, "해당 매칭을 처리할 권한이 없습니다.");
        }

        // 3. 이미 처리된 요청인지 확인
        if (matching.getStatus() != MatchingStatus.PENDING) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 처리된 매칭 요청입니다.");
        }

        // 4. 요청에 따라 상태 변경
        MatchingStatus newStatus = (requestDto.getMatchingStatus() == MatchingStatus.SUCCESS)
                ? MatchingStatus.SUCCESS
                : MatchingStatus.FAIL;

        // 5. receiver의 pqPoint 차감 or sender의 pqPoint 환급
        if (newStatus == MatchingStatus.FAIL) {
            pointService.chargePoints(matching.getSender(), matching.getSenderUsedPoints(), PointType.REFUND,
                    "매칭 거절 포인트 환불");

            // 거절 시, 거절한 사람(Receiver)이 거절 당한 사람(Sender)을 싫어요 목록에 추가
            if (!dislikeRepository.existsByFromUserAndToUser(matching.getReceiver(), matching.getSender())) {
                dislikeRepository.save(DislikeEntity.builder()
                        .fromUser(matching.getReceiver())
                        .toUser(matching.getSender())
                        .build());
            }
        } else if (newStatus == MatchingStatus.SUCCESS) {
            pointService.usePoints(matching.getReceiver(), matching.getReceiverUsedPoints(), "매칭 성공 포인트 차감");
        }

        matching.changeStatus(newStatus);

        // 알림 전송
        if (newStatus == MatchingStatus.SUCCESS) {
            notificationService.notify(matching.getSender(), NotificationType.MATCH_SUCCESS,
                    "매칭 성공!", matching.getReceiver().getNickname() + "님이 매칭을 수락했습니다! 연락처를 확인해보세요.",
                    "/matches/sent");
            notificationService.notify(matching.getReceiver(), NotificationType.MATCH_SUCCESS,
                    "매칭 성공!", matching.getSender().getNickname() + "님과 매칭되었습니다!",
                    "/matches/received");
        } else if (newStatus == MatchingStatus.FAIL) {
            notificationService.notify(matching.getSender(), NotificationType.MATCH_FAIL,
                    "매칭 거절", matching.getReceiver().getNickname() + "님이 매칭 요청을 거절했습니다. 포인트가 환불되었습니다.",
                    "/matches/sent");
        }

        return MatchingResponseDto.from(matching, currentId);
    }

    /**
     * 내가 보낸 매칭 요청 목록 조회
     * 
     * @param currentId 현재 로그인한 유저의 ID
     * @param pageable  페이징 정보 (page, size, sort)
     * @return 페이징 처리된 매칭 응답 DTO
     */
    public Page<MatchingResponseDto> getSentMatches(Long currentId, Pageable pageable) {
        Page<MatchingEntity> sentMatchesPage = matchingRepository.findBySenderIdWithUsers(currentId, pageable);

        // Page<Entity>를 Page<DTO>로 변환하여 반환
        return sentMatchesPage.map(matching -> MatchingResponseDto.from(matching, currentId));
    }

    /**
     * 내가 받은 매칭 요청 목록 조회
     * 
     * @param currentId 현재 로그인한 유저의 ID
     * @param pageable  페이징 정보 (page, size, sort)
     * @return 페이징 처리된 매칭 응답 DTO
     */
    public Page<MatchingResponseDto> getReceivedMatches(Long currentId, Pageable pageable) {
        Page<MatchingEntity> receivedMatchesPage = matchingRepository.findByReceiverIdWithUsers(currentId, pageable);

        // Page<Entity>를 Page<DTO>로 변환하여 반환
        return receivedMatchesPage.map(matching -> MatchingResponseDto.from(matching, currentId));
    }

    /**
     * 매칭 성공 후 연락처 교환
     *
     * @param matchId   연락처를 교환할 매칭의 ID
     * @param currentId 현재 로그인한 사용자의 ID
     * @return 파트너의 연락처 정보 (KakaoTalk ID, Instagram ID)
     */
    @Transactional
    public ContactExchangeResponseDto exchangeContact(Long matchId, Long currentId) {
        // 1. 매칭 정보 조회
        MatchingEntity matching = matchingRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "존재하지 않는 매칭 정보입니다."));

        // 2. 매칭 상태 검증: 매칭이 성공 상태인지 확인
        if (matching.getStatus() != MatchingStatus.SUCCESS) {
            throw new IllegalStateException("매칭이 성공한 상태에서만 연락처를 교환할 수 있습니다.");
        }

        // 3. 권한 검증: 현재 사용자가 매칭의 당사자인지 확인
        Long senderId = matching.getSender().getId();
        Long receiverId = matching.getReceiver().getId();

        if (!currentId.equals(senderId) && !currentId.equals(receiverId)) {
            throw new ForbiddenException(ErrorCode.NOT_OWNER, "연락처를 교환할 권한이 없습니다.");
        }

        // 4. 파트너 정보 조회
        UserEntity partner = currentId.equals(senderId) ? matching.getReceiver() : matching.getSender();

        // 5. 파트너의 연락처 정보를 DTO로 변환하여 반환
        return ContactExchangeResponseDto.from(partner);
    }

    /**
     * 성별에 따른 매칭 비용을 반환합니다.
     * 
     * @param gender 사용자의 성별
     * @return 해당 성별에 적용되는 매칭 비용 (포인트)
     */
    public int getMatchCostByGender(Gender gender) {
        return (gender == Gender.FEMALE) ? matchCostForWomen : matchCostForMen;
    }
}