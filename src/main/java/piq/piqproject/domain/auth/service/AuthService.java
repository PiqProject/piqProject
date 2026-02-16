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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.dto.CoordinateDto;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.ForbiddenException;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.error.exception.UnauthorizedException;
import piq.piqproject.common.util.IpUtil;
import piq.piqproject.config.jwt.JwtTokenProvider;
import piq.piqproject.domain.admin.log.entity.AdminAccessLogEntity;
import piq.piqproject.domain.admin.log.repository.AdminAccessLogRepository;
import piq.piqproject.domain.auth.dto.SocialUserInfo;
import piq.piqproject.domain.auth.dto.request.LoginRequestDto;
import piq.piqproject.domain.auth.dto.request.SignUpRequestDto;
import piq.piqproject.domain.auth.dto.response.SignUpResponseDto;
import piq.piqproject.domain.auth.dto.response.TokensResponseDto;
import piq.piqproject.domain.auth.entity.RefreshTokenEntity;
import piq.piqproject.domain.auth.repository.RefreshTokenRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Role;
import piq.piqproject.domain.users.enums.SocialType;
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.infra.external.kakao.dto.KakaoTokenResponse;
import piq.piqproject.infra.external.kakao.dto.KakaoUserInfoResponse;
import piq.piqproject.infra.external.kakao.service.KakaoGeocodingService;
import piq.piqproject.infra.external.kakao.service.KakaoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final KakaoGeocodingService kakaoGeocodingService;
    private final AdminAccessLogRepository adminAccessLogRepository;
    private static final Logger accessLogger = LoggerFactory.getLogger("UserAccessLogger");
    private final KakaoService kakaoService;

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
            throw new ConflictException(ErrorCode.ALREADY_EXISTS_USER);
        }

        // 1. 주소를 좌표로 변환
        CoordinateDto coordinate = kakaoGeocodingService.getCoordinate(signUpRequestDto.getAddress());

        // 좌표를 못 찾았을 때 예외 처리
        if (coordinate == null) {
            throw new InvalidRequestException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 주소입니다. 도로명 주소를 정확히 입력해주세요.");
        }

        // SRID 4326 = WGS84 (GPS 좌표계)
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // 주의: Coordinate(x, y) 순서이므로 (경도, 위도) 순서로 넣어야 합니다.
        Point location = geometryFactory
                .createPoint(new Coordinate(coordinate.getLongitude(), coordinate.getLatitude()));

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
                "자기소개",
                true, // isActive
                false, // isAppAlarm
                false, // isWebAlarm
                signUpRequestDto.getAddress(),
                location, // Point 객체 전달
                signUpRequestDto.getUniversity(),
                SocialType.NONE, // 소셜 타입
                null); // 소셜 ID

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
    @Transactional
    public TokensResponseDto login(LoginRequestDto loginRequestDto, HttpServletRequest request) {
        // 1. 이메일을 기반으로 사용자 조회
        UserEntity user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        // 2. 사용자의 비밀번호와 입력된 비밀번호가 일치하는지 확인
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new UnauthorizedException(PASSWORD_MISMATCH);
        }

        // 3. 계정 탈퇴 처리 중 확인
        if ((user.isWithdrawn())) { // UserEntity의 isEnabled() 메서드 활용
            throw new ForbiddenException(DISABLED_ACCOUNT_USER, "탈퇴된 계정입니다.");
        }

        // 4. 인증이 성공하면 JWT 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        String accessToken = jwtTokenProvider.createAccessToken(user);

        // 5. 생성된 Refresh Token을 Redis에 저장
        refreshTokenRepository.save(new RefreshTokenEntity(user.getId(), refreshToken));

        // 6. 로그인 로그 남기기
        String ip = IpUtil.getClientIp(request);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(userRole -> userRole.getRole() == Role.ADMIN);

        if (isAdmin) {
            AdminAccessLogEntity loginLog = AdminAccessLogEntity.builder()
                    .adminId(user.getId())
                    .adminEmail(user.getEmail())
                    .ip(ip)
                    .httpMethod("Login")
                    .url("/api/v1/auth/login")
                    .action("관리자 로그인")
                    .target("Login Success")
                    .build();

            adminAccessLogRepository.save(loginLog);
        }

        accessLogger.info("LOGIN | {} | {} | {}", user.getId(), user.getEmail(), ip);

        // 6. 생성된 토큰을 DTO에 담아 반환
        return TokensResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void logout(Long userId, HttpServletRequest request) {
        // 1. Redis에서 해당 사용자의 Refresh Token 삭제
        refreshTokenRepository.deleteById(userId);
        accessLogger.info("LOGOUT | {} | {}", userId, IpUtil.getClientIp(request));
    }

    /**
     * 회원 탈퇴 (계정 삭제)
     * 1. Redis에서 Refresh Token 삭제
     * 2. DB에서 UserEntity 삭제 (연관된 데이터는 Cascade에 의해 삭제됨)
     *
     * @param userId  탈퇴할 사용자의 ID
     * @param request IP 등의 로깅을 위한 HTTP 요청 객체
     */
    @Transactional
    public void withdraw(Long userId, HttpServletRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        // 1. Redis에서 Refresh Token 삭제
        refreshTokenRepository.deleteById(userId);

        // 2. Soft Delete 처리
        user.withdraw();

        // 3. 로그 기록
        accessLogger.info("WITHDRAW_PENDING | {} | {} | {}", userId, user.getEmail(), IpUtil.getClientIp(request));
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

        // 2. Refresh Token에서 사용자의 id을 추출
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 3. Redis에 저장된 Refresh Token을 id로 조회
        RefreshTokenEntity storedRefreshToken = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_REFRESH_TOKEN));

        // 4. 클라이언트로부터 받은 Refresh Token과 Redis에 저장된 토큰이 일치하는지 확인
        if (!storedRefreshToken.getRefreshToken().equals(refreshToken)) {
            throw new UnauthorizedException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. 새로운 Access Token을 생성하기 위해 사용자 정보를 DB에서 조회
        // (보안 상 이유로, 토큰에 모든 정보를 담기보다 DB에서 최신 정보를 가져오는 것이 안전)
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_USER));

        // 6. 새로운 Access Token을 생성하여 반환
        return jwtTokenProvider.createAccessToken(user);
    }

    // 유저 조회 및 생성 (핵심 로직)
    private UserEntity getOrCreateUser(SocialUserInfo socialInfo) {
        // A. 소셜 ID로 이미 가입된 유저인지 확인
        return userRepository.findBySocialTypeAndSocialId(socialInfo.getSocialType(), socialInfo.getSocialId())
                .map(this::checkWithdrawn)
                .orElseGet(() -> {
                    // B. 없으면 이메일로 가입된 유저가 있는지 확인 (계정 통합)
                    if (socialInfo.getEmail() != null) {
                        return userRepository.findByEmail(socialInfo.getEmail())
                                .map(this::checkWithdrawn)
                                .orElseGet(() -> createUser(socialInfo)); // C. 아예 없으면 신규 가입
                    }
                    return createUser(socialInfo);
                });
    }

    private UserEntity checkWithdrawn(UserEntity user) {
        if (user.isWithdrawn()) {
            throw new ForbiddenException(ErrorCode.DISABLED_ACCOUNT_USER,
                    "탈퇴 대기 중인 계정입니다. 7일 이후에 다시 가입해주세요.");
        }
        return user;
    }

    // 신규 유저 생성(getOrCreateUser에서 호출)
    private UserEntity createUser(SocialUserInfo socialInfo) {
        return userRepository.save(UserEntity.createSocialUser(
                socialInfo.getEmail(),
                socialInfo.getSocialType(),
                socialInfo.getSocialId()));
    }

    /**
     * [카카오 인가 코드 로그인]
     * 프론트엔드에서 받은 인가 코드(Authorization Code)를 사용하여
     * 백엔드에서 직접 카카오 토큰 발급 → 사용자 정보 조회 → 로그인/회원가입 처리
     *
     * @param code 카카오 인가 코드
     * @return TokensResponseDto (우리 서비스 JWT Access/Refresh Token)
     */
    @Transactional
    public TokensResponseDto kakaoLogin(String code) {

        // 1. 인가 코드로 카카오 Access Token 발급
        KakaoTokenResponse kakaoToken = kakaoService.getToken(code);

        // 2. 카카오 Access Token으로 사용자 정보 조회
        KakaoUserInfoResponse kakaoUser = kakaoService.getUserInfo(kakaoToken.accessToken());

        // 3. SocialUserInfo로 변환
        String email = null;
        if (kakaoUser.kakaoAccount() != null && kakaoUser.kakaoAccount().email() != null) {
            email = kakaoUser.kakaoAccount().email();
        }

        SocialUserInfo socialInfo = SocialUserInfo.builder()
                .socialId(String.valueOf(kakaoUser.id()))
                .email(email)
                .socialType(SocialType.KAKAO)
                .build();

        // 4. 회원 조회 또는 자동 회원가입
        UserEntity user = getOrCreateUser(socialInfo);

        // 5. 우리 서비스 전용 JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // 6. Refresh Token을 Redis에 저장 (덮어쓰기)
        refreshTokenRepository.save(new RefreshTokenEntity(user.getId(), refreshToken));

        // 7. 로그인 로그
        accessLogger.info("KAKAO_AUTH_CODE_LOGIN | {} | {}", user.getId(), user.getEmail());

        return new TokensResponseDto(accessToken, refreshToken);
    }

}