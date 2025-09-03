package piq.piqproject.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰을 검증하는 필터입니다.
 * OncePerRequestFilter를 상속받아, 클라이언트의 모든 요청에 대해 한 번씩만 실행되도록 보장합니다.
 */
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // HTTP 요청이 들어올 때마다 실행되는 메소드
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. HTTP 요청 헤더에서 JWT 토큰을 추출합니다.
        String token = resolveToken(request);

        // 2. TokenProvider를 사용하여 토큰의 유효성을 검증합니다.
        // 토큰이 존재하고, 유효성 검증에 성공한 경우에만 인증 정보를 처리합니다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰이 유효하면, 토큰에서 인증 정보를 가져옵니다.
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // 4. 가져온 인증 정보를 Spring Security의 SecurityContextHolder에 저장합니다.
            // SecurityContextHolder는 현재 실행 중인 스레드에 대한 보안 컨텍스트를 관리합니다.
            // 여기에 인증 정보가 저장되면, 해당 요청을 처리하는 동안 @PreAuthorize 등의 어노테이션 기반 보안 검사가 동작할 수 있습니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 다음 필터로 요청을 전달합니다.
        // JWT 검증 여부와 상관없이, 요청은 항상 다음 필터로 이어져야 합니다.
        // 만약 여기서 체인을 멈추면, 실제 API 컨트롤러까지 요청이 도달하지 못합니다.
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 'Authorization' 헤더를 찾아 Bearer 토큰을 추출하는 private 메소드입니다.
     *
     * @param request HttpServletRequest 객체
     * @return 추출된 JWT 토큰 문자열 (없거나 형식이 맞지 않으면 null 반환)
     */
    private String resolveToken(HttpServletRequest request) {
        // 'Authorization' 헤더 값을 가져옵니다.
        String authorizationHeader = request.getHeader("Authorization");

        // 헤더가 존재하고, 'Bearer '로 시작하는 경우에만 실제 토큰 부분을 추출합니다.
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // "Bearer " 다음의 문자열을 반환
        }

        return null; // 토큰이 없는 경우 null 반환
    }
}