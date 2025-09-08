package piq.piqproject.domain.users.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import piq.piqproject.common.error.exception.*;
import piq.piqproject.config.jwt.JwtTokenProvider;
import piq.piqproject.domain.users.dao.UserDao;
import piq.piqproject.domain.users.dto.LoginRequestDto;
import piq.piqproject.domain.users.dto.SignUpRequestDto;
import piq.piqproject.domain.users.dto.SignUpResponseDto;
import piq.piqproject.domain.users.dto.TokensResponseDto;
import piq.piqproject.domain.users.entity.RefreshTokenEntity;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.RefreshTokenRepository;
import piq.piqproject.domain.users.repository.UserRepository;

import static piq.piqproject.common.error.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * 회원가입 비즈니스 로직을 처리하는 메소드
     *
     * @param signUpRequestDto 회원가입 요청 DTO
     * @return 저장된 UserEntity
     */
    @Transactional // 데이터베이스에 변경 작업을 하므로 트랜잭션 처리
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        // 1. 이메일 중복 확인
        if (userDao.existsByEmail(signUpRequestDto.getEmail())) {
            // 실무에서는 custom exception을 정의하여 사용하는 것이 좋습니다.
            // ErrorCode Enum으로 정의해두었던 값을 사용하여 CustomException을 상속받고 있는 ConflictException에게
            // 넘겨줍니다.
            throw new ConflictException(ALREADY_EXISTS_USER);
        }

        // 2. DTO를 Entity로 변환 (이때 비밀번호 암호화가 이루어짐)
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());
        UserEntity userEntity = UserEntity.of(
                signUpRequestDto.getEmail(),
                encodedPassword,
                signUpRequestDto.getKakaoTalkId(),
                signUpRequestDto.getInstagramId(),
                signUpRequestDto.getAge(),
                signUpRequestDto.getGender(),
                signUpRequestDto.getMbti(),
                0.0,
                0,
                signUpRequestDto.getIntroduce(),
                true,
                Collections.singletonList("ROLE_USER"));

        // 3. 사용자 정보 저장
        userDao.saveUser(userEntity);

        return SignUpResponseDto.toDto(userEntity);
    }

    /**
     * 로그인 로직
     *
     * @param loginRequestDto 로그인 요청 DTO (email, password)
     * @return TokenResponse
     */
    @Transactional(readOnly = true)
    public TokensResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. 이메일을 기반으로 사용자 조회
        UserEntity user = userDao.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        // 2. 사용자의 비밀번호와 입력된 비밀번호가 일치하는지 확인
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new UnauthorizedException(PASSWORD_MISMATCH);
        }

        // 3. 계정 활성화 상태 확인
        if (!(user.isEnabled() && user.isAccountNonLocked())) { // UserEntity의 isEnabled() 메서드 활용
            throw new ForbiddenException(DISABLED_ACCOUNT_USER);
        }

        // 4. 인증이 성공하면 JWT 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        String accessToken = jwtTokenProvider.createAccessToken(user);

        // 5. 생성된 Refresh Token을 Redis에 저장
        refreshTokenRepository.save(new RefreshTokenEntity(user.getEmail(), refreshToken));

        // 6. 생성된 토큰을 DTO에 담아 반환
        return TokensResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     *
     * @param refreshToken
     * @return
     */
    @Transactional
    public void logout(String userEmail) {
        // 1. Redis에서 해당 사용자의 Refresh Token 삭제
        refreshTokenRepository.deleteById(userEmail);

    }

    /**
     * Access Token 재발급 로직
     *
     * @param refreshToken 클라이언트로부터 받은 Refresh Token
     * @return 새로 생성된 Access Token
     */
    @Transactional
    public String reissueAccessToken(String refreshToken) {
        // 1. Refresh Token의 유효성을 먼저 검증
        jwtTokenProvider.validateToken(refreshToken);

        // 2. Refresh Token에서 사용자의 이메일을 추출
        String userEmail = jwtTokenProvider.getUserEmail(refreshToken);

        // 3. Redis에 저장된 Refresh Token을 이메일로 조회
        RefreshTokenEntity storedRefreshToken = refreshTokenRepository.findById(userEmail)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_REFRESH_TOKEN));

        // 4. 클라이언트로부터 받은 Refresh Token과 Redis에 저장된 토큰이 일치하는지 확인
        storedRefreshToken.getRefreshToken().equals(refreshToken);

        // 5. 새로운 Access Token을 생성하기 위해 사용자 정보를 DB에서 조회
        // (보안 상 이유로, 토큰에 모든 정보를 담기보다 DB에서 최신 정보를 가져오는 것이 안전)
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        // 6. 새로운 Access Token을 생성하여 반환
        return jwtTokenProvider.createAccessToken(user);
    }

}