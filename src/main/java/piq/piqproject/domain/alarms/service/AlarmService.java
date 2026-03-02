package piq.piqproject.domain.alarms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.alarms.dto.request.DeviceTokenRequestDto;
import piq.piqproject.domain.alarms.entity.DeviceTokenEntity;
import piq.piqproject.domain.alarms.repository.DeviceTokenRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    // 1명당 허용할 최대 기기(토큰) 개수 (가비지 데이터 방지용)
    private static final int MAX_TOKEN_PER_USER = 5;

    @Transactional
    public void registerToken(Long userId, DeviceTokenRequestDto requestDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "사용자를 찾을 수 없습니다."));

        Optional<DeviceTokenEntity> existingToken = deviceTokenRepository.findByToken(requestDto.getToken());

        if (existingToken.isPresent()) {
            DeviceTokenEntity tokenEntity = existingToken.get();

            // 1. 디바이스 타입 검사 삭제! (어차피 같은 토큰이면 같은 기기임)
            // 2. 오직 "이 기기(토큰)를 이전에 쓰던 사람이 지금 로그인한 사람과 다른가?" 만 검사
            if (!tokenEntity.getUser().getId().equals(userId)) {
                // 다른 사람이 쓰던 PC/폰에서 내가 로그인 한 경우 -> 주인을 나로 바꿈
                deviceTokenRepository.delete(tokenEntity);
                createToken(user, requestDto);
            }
            // 주인이 같으면 이미 내 토큰이 잘 등록되어 있는 것이니 아무것도 안 함 (Return)
        } else {
            // DB에 처음 보는 새로운 토큰이면 그냥 추가 (1:N)
            createToken(user, requestDto);
        }
    }

    // ★ 핵심 수정: 유저의 전체 기기를 지우는 게 아니라, "특정 기기(Token) 1개만" 지우도록 변경
    @Transactional
    public void deleteToken(Long userId, String targetToken) {
        // userId와 token이 모두 일치하는 데이터 1개만 삭제
        Optional<DeviceTokenEntity> tokenEntity = deviceTokenRepository.findByUserIdAndToken(userId, targetToken);

        if (tokenEntity.isPresent()) {
            deviceTokenRepository.delete(tokenEntity.get());
            log.info("Token deleted for user: {}", userId);
        } else {
            log.warn("Tried to delete token but not found for user: {}", userId);
        }
    }

    private void createToken(UserEntity user, DeviceTokenRequestDto requestDto) {
        // 이미 가지고 있는 토큰 개수 확인
        List<DeviceTokenEntity> userTokens = deviceTokenRepository.findByUserIdOrderByCreatedAtAsc(user.getId());

        // MAX_TOKEN_PER_USER 개수 제한 체크 (5개 이상이면 가장 오래된 것 삭제)
        if (userTokens.size() >= MAX_TOKEN_PER_USER) {
            // 삭제할 개수 계산 (예: 현재 5개인데 1개 추가하려면 제일 오래된 1개 삭제)
            int excessCount = userTokens.size() - MAX_TOKEN_PER_USER + 1;
            for (int i = 0; i < excessCount; i++) {
                DeviceTokenEntity oldestToken = userTokens.get(i);
                deviceTokenRepository.delete(oldestToken);
                log.info("Deleted oldest device token to maintain limit (MAX {}). userId: {}, deleted token ID: {}",
                        MAX_TOKEN_PER_USER, user.getId(), oldestToken.getId());
            }
        }

        DeviceTokenEntity newToken = DeviceTokenEntity.builder()
                .user(user)
                .deviceType(requestDto.getDeviceType())
                .token(requestDto.getToken())
                .build();
        deviceTokenRepository.save(newToken);
        log.info("New device token registered for user: {}", user.getEmail());
    }

    // (기존의 deleteAlarm은 FCM 에러 시(UNREGISTERED) 호출되는 용도로 유지)
    @Transactional
    public void deleteAlarm(String targetToken) {
        Optional<DeviceTokenEntity> tokenEntity = deviceTokenRepository.findByToken(targetToken);
        tokenEntity.ifPresent(deviceTokenRepository::delete);
    }
}