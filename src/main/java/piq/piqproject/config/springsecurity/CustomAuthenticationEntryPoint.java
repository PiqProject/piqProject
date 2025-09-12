package piq.piqproject.config.springsecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        // 응답 상태 코드를 401 Unauthorized로 설정합니다.
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // 응답의 Content-Type을 JSON으로 설정합니다.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 응답의 문자 인코딩을 UTF-8로 설정합니다.
        response.setCharacterEncoding("UTF-8");

        // 응답 바디에 담을 에러 메시지를 생성합니다.
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "인증이 필요합니다. 로그인을 해주세요.");
        errorDetails.put("path", request.getRequestURI());

        // ObjectMapper를 사용하여 Map을 JSON 문자열로 변환하고, 응답 바디에 씁니다.
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}