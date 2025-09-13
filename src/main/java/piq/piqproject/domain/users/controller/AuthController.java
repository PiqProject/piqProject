package piq.piqproject.domain.users.controller;

import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.users.dto.AccessTokenResponseDto;
import piq.piqproject.domain.users.dto.LoginRequestDto;
import piq.piqproject.domain.users.dto.SignUpRequestDto;
import piq.piqproject.domain.users.dto.TokensResponseDto;
import piq.piqproject.domain.users.service.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

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

        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    /**
     * 클라이언트에게 두개의 토큰을 어떻게 전달할까
     * -> RefreshToken을 쿠키에,AccessToken은 body에 담아 반환
     * 
     * @param loginRequestDto
     * @return ResponseEntity<AccessTokenResponseDto>
     */
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        log.debug("User login attempt: {}", loginRequestDto.getEmail());

        TokensResponseDto tokenResponseDto = authService.login(loginRequestDto);

        // 1. Refresh Token을 위한 HttpOnly 쿠키 생성
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponseDto.getRefreshToken())
                .maxAge(7 * 24 * 60 * 60) // 쿠키 수명 7일로 설정
                .path("/") // 모든 경로에서 쿠키 사용
                // .secure(true) // HTTPS 환경에서만 쿠키 전송
                .sameSite("None") // 다른 도메인에서도 쿠키 전송 허용 (CORS 환경)?
                .httpOnly(true) // JavaScript 접근 방지
                .build();

        // 2. Access Token만 포함하는 응답 DTO 생성
        AccessTokenResponseDto accessTokenResponse = new AccessTokenResponseDto(tokenResponseDto.getAccessToken());

        // 3. 최종 응답 생성: 헤더에는 쿠키를, 바디에는 Access Token을 담아 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(accessTokenResponse);
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
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {

        // 1. 현재 인증된 사용자의 이메일(username)을 가져옵니다.
        String userEmail = userDetails.getUsername();

        // 2. 서비스 레이어에 로그아웃 처리를 위임. (Redis에서 Refresh Token 삭제)
        authService.logout(userEmail);

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
     * Refresh Token을 이용하여 새로운 Access Token 발급
     * 
     * @param refreshToken
     * @return ResponseEntity<AccessTokenResponseDto>
     */
    @PostMapping("/reissue")
    public ResponseEntity<AccessTokenResponseDto> reissue(@CookieValue("refreshToken") String refreshToken) {
        log.info("reissue 요청이 controller에 도달");
        // 1. Refresh Token 유효성 검사 및 새로운 Access Token 발급
        String newAccessToken = authService.reissueAccessToken(refreshToken);
        // 2. 새로운 Access Token을 응답 DTO에 저장
        AccessTokenResponseDto responseDto = new AccessTokenResponseDto(newAccessToken);
        // 3. 새로운 Access Token을 포함한 응답 반환
        return ResponseEntity.ok().body(responseDto);
    }
}
