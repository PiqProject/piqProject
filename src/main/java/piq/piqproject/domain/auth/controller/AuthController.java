package piq.piqproject.domain.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.auth.dto.request.LoginRequestDto;
import piq.piqproject.domain.auth.dto.request.ReissueRequestDto;
import piq.piqproject.domain.auth.dto.request.SignUpRequestDto;
import piq.piqproject.domain.auth.dto.request.SocialLoginRequestDto;
import piq.piqproject.domain.auth.dto.response.AccessTokenResponseDto;
import piq.piqproject.domain.auth.dto.response.TokensResponseDto;
import piq.piqproject.domain.auth.service.AuthService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
// TODO: 기존 회원가입로직 버려야함(관리자 로그인을 위해 로그인은 놔두기)
public class AuthController {

    private final AuthService authService;

    @Value("${refreshToken.maxAge}")
    private int COOKIEMAXAGE;

    /**
     * ※잘못된 email을 넣을시 MethodArgumentNotValidException을 발생시켜 400 Bad Request 에러를
     * 응답해야하는데 그대로 DB에 저장하는 문제있음
     * 
     * @param signUpRequestDto
     * @return ResponseEntity<String> (회원가입 성공 메시지)
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {

        authService.signUp(signUpRequestDto);
        log.info("New user registered: {}", signUpRequestDto.getEmail());

        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    /**
     * 관리자가 로그인 시에 쓸 API
     * 하이브리드 방식 적용: 쿠키(Web) + JSON Body(App) 모두 refreshToken 포함+ json엔 accessToken도
     * 존재
     */
    @PostMapping("/login")
    public ResponseEntity<TokensResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
            HttpServletRequest request) {

        TokensResponseDto tokenResponseDto = authService.login(loginRequestDto, request);
        log.info("Admin/User login attempt: {}", loginRequestDto.getEmail());

        // 1. Refresh Token을 위한 HttpOnly 쿠키 생성 (Web용)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponseDto.getRefreshToken())
                .maxAge(COOKIEMAXAGE)
                .path("/")
                .sameSite("None")
                .httpOnly(true)
                .secure(true) // HTTPS 필수
                .build();

        // 2. 최종 응답: 헤더(쿠키) + 바디(Access/Refresh 둘 다)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokenResponseDto); // TokensResponseDto에는 둘 다 들어있음
    }

    /**
     * 1.Redis에 저장된 Refresh Token을 삭제하여, 해당 토큰으로는 더 이상 새로운 Access Token을 발급받지 못하게 함
     * 2.브라우저에 저장된 Refresh Token 쿠키를 삭제하도록 명령하여, 사용자의 브라우저를 깨끗한 상태로 만듦
     * ★로그아웃 로직은 클라이언트의 Access Token은 무효화하지않음, 클라이언트에서 처리해야함
     * ★클라이언트가 /logout API를 사용시 이전에 로그인 시 발급받았던 유효한 Access Token을 Authorization
     * 헤더에 담아 보내기만하면됨
     * 
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserEntity user, HttpServletRequest request) {

        // 1. 현재 인증된 사용자의 ID를 가져옵니다.
        Long userId = user.getId();

        // 2. 서비스 레이어에 로그아웃 처리를 위임. (Redis에서 Refresh Token 삭제)
        authService.logout(userId, request);
        log.info("User logout attempt: {}", userId);

        // 3. 클라이언트 측의 Refresh Token 쿠키를 삭제하기 위한 쿠키를 생성
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", null)
                .maxAge(0) // 쿠키의 수명을 0으로 설정하여 즉시 만료시킵니다.
                .path("/")
                // .secure(true), .sameSite("None"), .httpOnly(true) 등 기존 쿠키와 동일한 속성을 유지해야
                // 브라우저가 동일한 쿠키로 인식하고 삭제
                // .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();

        // 4. 응답 헤더에 쿠키 삭제 명령을 추가하고, 성공 메시지를 바디에 담아 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("로그아웃 되었습니다.");
    }

    /**
     * [토큰 재발급] - 하이브리드 요청 처리
     * 쿠키(웹) 또는 바디(앱) 둘 중 하나에서 토큰을 추출하여 처리
     */
    @PostMapping("/reissue")
    public ResponseEntity<AccessTokenResponseDto> reissue(
            @CookieValue(value = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) ReissueRequestDto reissueRequestDto) {

        log.info("Reissue request received");

        // 1. 토큰 추출 우선순위 로직
        // 쿠키가 있으면 쿠키 사용, 없으면 Body에서 추출
        String refreshToken = null;

        if (cookieRefreshToken != null && !cookieRefreshToken.isBlank()) {
            refreshToken = cookieRefreshToken;
        } else if (reissueRequestDto != null && reissueRequestDto.getRefreshToken() != null) {
            refreshToken = reissueRequestDto.getRefreshToken();
        }

        // 2. 토큰이 둘 다 없으면 에러 처리
        if (refreshToken == null) {
            throw new NotFoundException(ErrorCode.NOT_FOUND, "리프레시 토큰이 쿠키나 바디에 없습니다.");
        }

        // 3. 재발급 서비스 호출
        String newAccessToken = authService.reissueAccessToken(refreshToken);

        // 4. 응답 (Access Token만 반환)
        // (만약 RTR-Refresh Token Rotation을 적용한다면 여기서도 쿠키/바디 갱신해줘야 함)
        return ResponseEntity.ok(new AccessTokenResponseDto(newAccessToken));
    }

    /**
     * [소셜 로그인]
     * 1. 웹을 위해 HttpOnly 쿠키 설정
     * 2. 앱을 위해 JSON Body에도 Refresh Token 포함
     */
    @PostMapping("/login/social")
    public ResponseEntity<TokensResponseDto> socialLogin(@RequestBody @Valid SocialLoginRequestDto request) {

        // 1. 서비스 로직 수행 (토큰 발급)
        TokensResponseDto tokens = authService.socialLogin(request);

        // 2. 쿠키 생성 (웹용)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .maxAge(COOKIEMAXAGE) // 14일
                .path("/")
                .sameSite("None")
                .httpOnly(true)
                .secure(true)
                .build();

        // 3. 헤더(쿠키) + 바디(토큰 2개 모두) 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens);
    }

}
