package piq.piqproject.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.CustomException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 다음 필터(JwtFilter)를 실행합니다.
            filterChain.doFilter(request, response);
        } catch (CustomException ex) {
            // JwtFilter에서 발생한 예외를 여기서 처리합니다.
            setErrorResponse(request, response, ex);
        }
    }

    private void setErrorResponse(HttpServletRequest request, HttpServletResponse response, CustomException ex)
            throws IOException {
        log.info("JwtExceptionFilter is operated:  {}", ex.getMessage());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(ex.getErrorCode().getStatus().value());

        final Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getErrorCode().getStatus().value());
        body.put("error", ex.getErrorCode().getStatus().getReasonPhrase());
        body.put("message", ex.getMessage()); // JwtTokenProvider에서 설정한 예외 메시지가 담깁니다.
        body.put("path", request.getRequestURI());

        mapper.writeValue(response.getOutputStream(), body);
    }
}