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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/*
 *  인증(Authentication): "당신은 누구입니까?" (신분 확인)
 *  권한(Authority/Role): 수많은 자원 접근 규칙들을 묶어서 관리하기 쉽게 만든 '문자열 라벨' (정보). (예: "ROLE_USER")
 *  인가(Authorization): 클라이언트가 특정 자원에 접근을 시도할 때, 그 클라이언트가 가진 **권한(라벨)**을 보고, SecurityConfig에 정의된 규칙서에 따라 접근을 허용하거나 거부하는 '행위'.
 */
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    // jwt속성을 갖고있는 클래스 주입
    private final JwtProperties jwtProperties;
    // JWT secret key
    private Key key;
    // JWT parser, 토큰 검증 시 사용
    private JwtParser jwtParser;
    // UserDetailsService 주입
    private final UserDetailsService userDetailsService;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
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

        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // 헤더 typ : JWT
                .setIssuer(jwtProperties.getIssuer()) // 발급자 정보
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiry) // 만료 시간
                .setSubject(user.getEmail()) // 토큰 제목 (사용자 식별값)
                .claim("id", user.getId()) // 비공개 클레임(사용자 정의 클레임) key-value 형태로 추가 정보 저장
                .claim("auth", authorities)
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
            jwtParser.parseClaimsJws(token);
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
     * token을 받아 Spring security가 이해하는 Authentication객체로 변환
     *
     * @param token 인증 정보가 담긴 JWT 토큰
     * @return Spring Security가 이해하는 형태의 Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        // 2. 토큰에서 사용자의 이메일(Subject)을 추출합니다.
        String userEmail = getClaims(token).getSubject();

        // 3. UserDetailsService를 통해 DB에서 실제 UserEntity(UserDetails) 객체를 조회합니다.
        // 이 과정에서 매 요청마다 DB 조회가 발생합니다.
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        // 4. 조회된 UserDetails(실제로는 UserEntity 객체)를 principal로 사용하여 Authentication 객체를
        // 생성합니다.
        // 권한 정보는 DB에서 조회한 userDetails 객체의 것을 사용하므로, 토큰의 권한 정보는 사용하지 않습니다.
        return new UsernamePasswordAuthenticationToken(
                userDetails, // principal: DB에서 조회한 UserEntity 객체
                "", // credentials: 비밀번호는 보통 비워둡니다.
                userDetails.getAuthorities()); // authorities: DB에서 조회한 실제 권한 목록
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
