package piq.piqproject.domain.verification.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.admin.dto.response.UserVerificationResponseDto;
import piq.piqproject.domain.verification.repository.VerificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerifiationService {

    private final VerificationRepository verificationRepository;

    /**
     * 현재 로그인한 유저 자신의 검증 목록(이미지, 음성) 조회
     */
    public List<UserVerificationResponseDto> getMyVerifications(Long userId) {
        log.info("User {} requested own verification list.", userId);
        return verificationRepository.findAllByUserId(userId)
                .stream()
                .map(UserVerificationResponseDto::of)
                .collect(Collectors.toList());
    }
}
