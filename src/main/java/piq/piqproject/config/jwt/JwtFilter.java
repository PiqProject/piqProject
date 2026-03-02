package piq.piqproject.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰을 검증하는 필터입니다.
 * OncePerRequestFilter를 상속받아, 클라이언트의 모든 요청에 대해 한 번씩만 실행되도록 보장합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    // HTTP 요청이 들어올 때마다 실행되는 메소드
    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰이 존재할 때만 검증을 시도하되, 실패하더라도 다음 필터로 진행하게 함
        if (token != null) {
            try {
                // validateToken이 예외를 던지는 구조라면 여기서 try-catch로 잡습니다.
                jwtTokenProvider.validateToken(token);

                // 유효하다면 인증 객체 생성 및 Context 저장
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT token authentication successful: {}", authentication.getName());
            } catch (Exception e) {
                // 예외가 발생해도 로그만 남기고 아무것도 하지 않음
                // SecurityContext에 Authentication을 세팅하지 않는 것만으로 충분
                log.debug("Invalid JWT token: {}", e.getMessage());
            }
        }

        // 예외가 발생했든, 토큰이 없었든 '무조건' 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }

    /**
     * 
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