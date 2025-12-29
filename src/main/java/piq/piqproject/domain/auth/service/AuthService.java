package piq.piqproject.domain.auth.service;

import static piq.piqproject.common.error.exception.ErrorCode.DISABLED_ACCOUNT_USER;
import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_REFRESH_TOKEN;
import static piq.piqproject.common.error.exception.ErrorCode.NOT_FOUND_USER;
import static piq.piqproject.common.error.exception.ErrorCode.PASSWORD_MISMATCH;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.dto.CoordinateDto;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.ForbiddenException;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.error.exception.UnauthorizedException;
import piq.piqproject.config.jwt.JwtTokenProvider;
import piq.piqproject.domain.auth.dto.request.LoginRequestDto;
import piq.piqproject.domain.auth.dto.request.SignUpRequestDto;
import piq.piqproject.domain.auth.dto.response.SignUpResponseDto;
import piq.piqproject.domain.auth.dto.response.TokensResponseDto;
import piq.piqproject.domain.auth.entity.RefreshTokenEntity;
import piq.piqproject.domain.auth.repository.RefreshTokenRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.infra.external.kakao.service.KakaoGeocodingService;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoGeocodingService kakaoGeocodingService;

    /**
     * 회원가입 비즈니스 로직을 처리하는 메소드
     *
     * @param signUpRequestDto 회원가입 요청 DTO
     * @return 저장된 UserEntity
     */
    @Transactional
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        // 0. 이메일 및 닉네임 중복 확인 (기존 로직 유지)
        if (userRepository.existsByEmail(signUpRequestDto.getEmail())
                || userRepository.existsByNickname(signUpRequestDto.getNickname())) {
            throw new ConflictException(ErrorCode.ALREADY_EXISTS_USER); // ErrorCode 변수명에 맞게 수정 필요
        }

        // 1. 주소를 좌표로 변환
        CoordinateDto coordinate = kakaoGeocodingService.getCoordinate(signUpRequestDto.getAddress());

        // 좌표를 못 찾았을 때 예외 처리
        if (coordinate == null) {
            throw new InvalidRequestException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 주소입니다. 도로명 주소를 정확히 입력해주세요.");
        }

        // ▼▼▼ [수정] Point 객체 생성 (JTS) ▼▼▼
        // SRID 4326 = WGS84 (GPS 좌표계)
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // 주의: Coordinate(x, y) 순서이므로 (경도, 위도) 순서로 넣어야 합니다.
        Point location = geometryFactory
                .createPoint(new Coordinate(coordinate.getLongitude(), coordinate.getLatitude()));
        // ▲▲▲ [수정] ▲▲▲

        // 2. DTO를 Entity로 변환
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

        // UserEntity.of 메서드도 파라미터가 Point를 받도록 수정되어 있어야 합니다.
        UserEntity userEntity = UserEntity.of(
                signUpRequestDto.getEmail(),
                signUpRequestDto.getNickname(),
                encodedPassword,
                signUpRequestDto.getKakaoTalkId(),
                signUpRequestDto.getInstagramId(),
                signUpRequestDto.getAge(),
                signUpRequestDto.getGender(),
                signUpRequestDto.getMbti(),
                0.0, // totalScore
                0, // pqPoint (가입 시 기본 포인트, 필요하면 수정)
                signUpRequestDto.getIntroduce(),
                true, // isActive
                signUpRequestDto.getAddress(),
                location, // Point 객체 전달
                signUpRequestDto.getUniversity());

        // 3. 사용자 정보 저장
        userRepository.save(userEntity);

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
        UserEntity user = userRepository.findByEmail(loginRequestDto.getEmail())
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