package piq.piqproject.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.*;
import piq.piqproject.domain.users.entity.UserEntity;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/*
 *  인증(Authentication): "당신은 누구입니까?" (신분 확인)
 *  권한(Authority/Role): 수많은 자원 접근 규칙들을 묶어서 관리하기 쉽게 만든 '문자열 라벨' (정보). (예: "ROLE_USER")
 *  인가(Authorization): 클라이언트가 특정 자원에 접근을 시도할 때, 그 클라이언트가 가진 **권한(라벨)**을 보고, SecurityConfig에 정의된 규칙서에 따라 접근을 허용하거나 거부하는 '행위'.
 */
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    // jwt속성을 갖고있는 클래스 주입
    private final JwtProperties jwtProperties;
    // JWT secret key
    private Key key;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 사용자 정보와 만료 기간을 기반으로 JWT 토큰을 생성.
     *
     * @param user User Entity
     * @return 받은 User Entity에 대한 JWT 토큰 문자열
     * @author PJT
     */
    public String createAccessToken(UserEntity user) {
        // JWT 토큰 생성 로직
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // 헤더 typ : JWT
                .setIssuer(jwtProperties.getIssuer()) // 발급자 정보
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiry) // 만료 시간
                .setSubject(user.getEmail()) // 토큰 제목 (사용자 식별값)
                .claim("id", user.getId()) // 비공개 클레임(사용자 정의 클레임) key-value 형태로 추가 정보 저장
                .claim("auth", user.getRoles())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     * Refresh Token은 사용자 식별 정보 외에 다른 claim을 담지 않는 것이 일반적
     * 
     * @author PJT
     * @param UserEntity user
     * @return 받은 User Entity에 대한 JWT 토큰 문자열
     */
    public String createRefreshToken(UserEntity user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setSubject(user.getEmail()) // Access Token 재발급 시 사용자 식별을 위해 필요 - redis에서 key값으로 사용
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰의 3가지 유효성(서명, 만료시간, 구조)을 검증
     *
     * @param token 검사할 JWT 토큰(String)
     * @return 토큰이 유효하면 true, 아니면 false
     * @author PJT
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder() // JWT parsing (해석기) 생성을 위한 빌더
                    .setSigningKey(key) // 서명을 검증할 때 사용할 비밀키 설정
                    .build() // parser instance 생성
                    .parseClaimsJws(token); // parser로 token인증 -서명 검증, 만료시간검증, 구문분석(JWT형식인지) ->하나라도 이상하면 exception
            return true;
        } catch (SignatureException e) {
            throw new UnauthorizedException(ErrorCode.INVALID_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(ErrorCode.JWT_TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new UnauthorizedException(ErrorCode.UNSUPPORTED_JWT_TOKEN);
        } catch (MalformedJwtException e) {
            throw new UnauthorizedException(ErrorCode.MALFORMED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(ErrorCode.JWT_TOKEN_MISSING);
        } catch (Exception e) {
            throw new InternalServerException(ErrorCode.JWT_PROCESSING_ERROR);
        }
    }

    /**
     * token을 받아 spring security가 이해하는 Authentication객체로 변환
     * <p>
     * Spring security는 jwt를 직접 이해 못함. security는 Authentication이라는 객체를 통해 현재 사용자가
     * 누구인가를 판단함 따라서 token(암호화된 문자열)을 Authentication 객체로 변환해야 함
     *
     * @param token 인증 정보가 담긴 JWT 토큰
     * @return Spring Security가 이해하는 형태의 Authentication 객체
     * @author PJT
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token); // Claims는 key-value로 데이터를 담고있음
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("auth", List.class); // "auth"의 값이 2개이상일 수 있으므로 List로 받음
        // spring security는 권한을 grantedAuthority interface로 표현, simpleGrantedAuthority는
        // 그 간단한 구현체
        Set<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new) // 각 role 문자열을 new SimpleGrantedAuthority("role")로 변환
                .collect(Collectors.toSet());

        // claims.getSubject()는 토큰의 제목, 즉 사용자 이메일을 반환합니다.
        // new org.springframework.security.core.userdetails.User 객체를 사용해 인증 객체를 만듭니다.
        // 비밀번호는 인증된 상태이므로 빈 문자열("")을 넣습니다.
        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities),
                token,
                authorities);
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다.
     *
     * @param token 사용자 정보가 담긴 JWT 토큰
     * @return 토큰에서 추출한 사용자 ID(Long 타입)
     * @author PJT
     */
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);
    }

    /**
     * 토큰에서 Subject(사용자 이메일)를 추출합니다.
     */
    public String getUserEmail(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰의 클레임 정보(payload의 부분)를 추출하는 private 메소드
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
