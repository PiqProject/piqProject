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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 디바이스 토큰을 등록하거나 업데이트합니다.
     */
    @Transactional
    public void registerToken(Long userId, DeviceTokenRequestDto requestDto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "사용자를 찾을 수 없습니다."));

        // 동일한 토큰이 이미 존재하는지 확인
        Optional<DeviceTokenEntity> existingToken = deviceTokenRepository.findByToken(requestDto.getToken());

        if (existingToken.isPresent()) {
            DeviceTokenEntity tokenEntity = existingToken.get();
            // 토큰 주인이 바뀌었거나 디바이스 정보가 변경된 경우 업데이트
            if (!tokenEntity.getUser().getId().equals(userId)
                    || !tokenEntity.getDeviceType().equals(requestDto.getDeviceType())) {
                deviceTokenRepository.delete(tokenEntity);
                createToken(user, requestDto);
            }
        } else {
            // 새 토큰 등록
            createToken(user, requestDto);
        }
    }

    @Transactional
    public void deleteToken(Long userId) {
        DeviceTokenEntity tokenEntity = deviceTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "등록된 디바이스 토큰이 없습니다."));

        deviceTokenRepository.delete(tokenEntity);
    }

    private void createToken(UserEntity user, DeviceTokenRequestDto requestDto) {

        DeviceTokenEntity newToken = DeviceTokenEntity.builder()
                .user(user)
                .deviceType(requestDto.getDeviceType())
                .token(requestDto.getToken())
                .build();
        deviceTokenRepository.save(newToken);
        log.info("New device token registered for user: {}", user.getEmail());
    }

    public void deleteAlarm(String targetToken) {
        DeviceTokenEntity tokenEntity = deviceTokenRepository.findByToken(targetToken)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "등록된 디바이스 토큰이 없습니다."));

        deviceTokenRepository.delete(tokenEntity);
    }
}
