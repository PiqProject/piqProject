package piq.piqproject.config.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 향후 만들 JWT 필터를 import 해야 합니다.
// import piq.piqproject.security.JwtAuthenticationFilter; 

@Configuration
@EnableWebSecurity // Spring Security를 활성화하고 웹 보안 설정을 구성함을 나타냅니다.
@RequiredArgsConstructor
public class SecurityConfig {
    // JWT 토큰 제공자 및 필터를 주입받습니다.
    private final JwtFilter jwtFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    // [추가] 인증 없이 접근을 허용할 경로 목록
    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/api/v1/user/profiles",
            "/h2-console/**", // H2 콘솔 접근 허용
            "/swagger-ui/**", // Swagger UI 접근 허용
            "/v3/api-docs/**", // Swagger API 문서 접근 허용
    };

    // 1. 비밀번호 암호화를 위한 PasswordEncoder Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. 인증을 총괄하는 AuthenticationManager Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 3. HTTP 보안 설정을 위한 SecurityFilterChain Bean 등록
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다.
        // REST API는 세션을 사용하지 않고 JWT 토큰을 사용하므로, 일반적으로 CSRF 보호가 필요 없습니다.
        http.csrf(AbstractHttpConfigurer::disable);

        // HTTP Basic 인증 방식을 비활성화합니다.
        // 헤더에 사용자 이름과 비밀번호를 인코딩하여 보내는 방식 대신 JWT를 사용합니다.
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 폼 기반 로그인 방식을 비활성화합니다.
        // 서버가 제공하는 로그인 폼 페이지 대신, 클라이언트가 직접 로그인 요청을 보내도록 합니다.
        http.formLogin(AbstractHttpConfigurer::disable);

        // 세션 관리 정책을 STATELESS로 설정합니다.
        // 이는 서버가 세션을 생성하거나 사용하지 않음을 의미하며, 모든 요청을 독립적으로 처리합니다. (JWT의 핵심)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // HTTP 요청에 대한 접근 권한을 설정합니다.
        http.authorizeHttpRequests(authorize -> authorize
                // "/api/signup", "/api/login" 엔드포인트는 인증 없이 누구나 접근할 수 있도록 허용합니다.
                .requestMatchers(AUTH_WHITELIST)
                .permitAll()
                // 그 외의 모든 요청은 반드시 인증(로그인)된 사용자만 접근할 수 있도록 설정합니다.
                .anyRequest().authenticated())

                // [추가] H2 콘솔을 위한 헤더 설정
                // H2 콘솔은 iframe을 사용하므로, X-Frame-Options 헤더를 비활성화하거나 동일 출처(sameOrigin)로 설정해야
                // 합니다.
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()));

        // 다른 필터를 추가할 경우 여기에 추가할것
        // JWT Filter(custom Filter)를 Spring Security 이전에 추가
        http.addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}