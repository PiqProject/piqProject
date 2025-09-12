package piq.piqproject.config.springsecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 응답 상태 코드를 403 Forbidden으로 설정합니다.
        response.setStatus(HttpStatus.FORBIDDEN.value());
        // 응답의 Content-Type을 JSON으로 설정합니다.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 응답의 문자 인코딩을 UTF-8로 설정합니다.
        response.setCharacterEncoding("UTF-8");

        // 응답 바디에 담을 에러 메시지를 생성합니다.
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpStatus.FORBIDDEN.value());
        errorDetails.put("error", "Forbidden");
        errorDetails.put("message", "접근 권한이 없습니다.");
        errorDetails.put("path", request.getRequestURI());

        // ObjectMapper를 사용하여 Map을 JSON 문자열로 변환하고, 응답 바디에 씁니다.
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}