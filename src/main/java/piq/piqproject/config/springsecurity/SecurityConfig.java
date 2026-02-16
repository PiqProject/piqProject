package piq.piqproject.config.springsecurity;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import piq.piqproject.config.jwt.JwtFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    // JWT 토큰 제공자 및 필터를 주입받습니다.
    private final JwtFilter jwtFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    // 인증 없이 접근을 허용할 경로 목록
    private static final String[] AUTH_WHITELIST = {
            "/home",
            "/uploads/**",
            "/webhook/**", // 외부 결제 플랫폼 웹훅 (Google, Apple)
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/api/v1/auth/login/kakao",
            "/api/v1/faqs",
            "/api/v1/reviews",
            "/api/v1/posts/**",
            "/api/v1/products/**",
            "/api/v1/interests/**",
            "/api/v1/traits/**"
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
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 이 부분 추가
                .csrf(csrf -> csrf.disable());
        // HTTP Basic 인증 방식을 비활성화합니다.
        // 헤더에 사용자 이름과 비밀번호를 인코딩하여 보내는 방식 대신 JWT를 사용합니다.
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 폼 기반 로그인 방식을 비활성화합니다.
        // 서버가 제공하는 로그인 폼 페이지 대신, 클라이언트가 직접 로그인 요청을 보내도록 합니다.
        http.formLogin(AbstractHttpConfigurer::disable);

        // 세션 관리 정책을 STATELESS로 설정합니다.
        // 이는 서버가 세션을 생성하거나 사용하지 않음을 의미하며, 모든 요청을 독립적으로 처리합니다. (JWT의 핵심)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패 시 처리
                .accessDeniedHandler(customAccessDeniedHandler) // 인가 실패 시 처리
        );

        // HTTP 요청에 대한 접근 권한을 설정합니다.
        http.authorizeHttpRequests(authorize -> authorize
                // 1. [Public] 누구나 접근 가능
                .requestMatchers(AUTH_WHITELIST).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll() // 게시글 읽기 허용

                // 2. [GUEST + USER] 신규 가입자(GUEST)도 프로필 설정은 해야 함
                .requestMatchers("/api/v1/auth/logout").hasAnyRole("GUEST", "USER") // 유저 로그아웃
                .requestMatchers("/api/v1/users/**").hasAnyRole("GUEST", "USER") // 유저 프로필 정보 업데이트
                .requestMatchers("/api/v1/inquiries/**").hasAnyRole("GUEST", "USER") // 문의
                .requestMatchers("/api/v1/payments/**").hasAnyRole("GUEST", "USER") // 결제

                // 3. [USER 전용] 핵심 비즈니스 로직 (GUEST 접근 불가 -> 프로필 입력 강제)
                .requestMatchers("/api/v1/recommendations/**").hasRole("USER") // 추천
                .requestMatchers("/api/v1/matches/**").hasRole("USER") // 매칭
                .requestMatchers("/api/v1/reports/**").hasRole("USER") // 신고
                .requestMatchers("/api/v1/reviews/**").hasRole("USER") // 리뷰

                // 4. [ADMIN 전용] 관리자 기능
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // 5. 그 외 모든 요청은 '인증'만 되어 있으면 통과 (GUEST도 접근 가능할 수 있음)
                // 만약 GUEST를 철저히 막고 싶다면, 위에서 명시하지 않은 건 ADMIN/USER만 가능하게 설정 고려
                .anyRequest().authenticated());

        // 다른 필터를 추가할 경우 여기에 추가할것
        // JWT Filter(custom Filter)를 Spring Security 이전에 추가
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 리액트(프론트) 주소 허용
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // 2. GET, POST, PUT, DELETE 등 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3. 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));

        // 4. 인증 정보(쿠키/토큰) 포함 허용 (로그인 기능 시 필수)
        configuration.setAllowCredentials(true);

        // 5. 브라우저가 헤더에 접근할 수 있게 노출 (선택사항, JWT 쓸 때 필요할 수 있음)
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}