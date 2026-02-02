package piq.piqproject.infra.external.apple.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
// application.properties의 apple.iap.enabled 값이 "true"일 때만 이 빈을 생성함
// 값이 false거나 아예 없으면(matchIfMissing = false) 빈 등록을 안 함 -> 에러 안 남
@ConditionalOnProperty(name = "apple.iap.enabled", havingValue = "true")
public class AppleStoreClientService {

        @Value("${apple.appstore.issuer-id}")
        private String issuerId;

        @Value("${apple.appstore.key-id}")
        private String keyId;

        @Value("${apple.appstore.private-key-path}")
        private String privateKeyPath;

        @Value("${apple.appstore.bundle-id}")
        private String bundleId;

        @Value("${apple.appstore.environment:sandbox}")
        private String environment;

        private WebClient webClient;
        private ECPrivateKey privateKey;

        @PostConstruct
        public void init() throws Exception {
                this.webClient = WebClient.builder()
                                .baseUrl(environment.equals("production")
                                                ? "https://api.storekit.itunes.apple.com"
                                                : "https://api.storekit-sandbox.itunes.apple.com")
                                .build();

                // 1. ClassPathResource 생성
                ClassPathResource resource = new ClassPathResource(privateKeyPath);

                // JAR 파일 내부에서도 문제없이 읽을 수 있는 방식입니다.
                try (InputStream inputStream = resource.getInputStream()) {
                        byte[] keyBytes = inputStream.readAllBytes();
                        String keyContent = new String(keyBytes, StandardCharsets.UTF_8)
                                        .replace("-----BEGIN PRIVATE KEY-----", "")
                                        .replace("-----END PRIVATE KEY-----", "")
                                        .replaceAll("\\s+", ""); // 줄바꿈, 공백 제거

                        byte[] encoded = Base64.getDecoder().decode(keyContent);
                        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
                        KeyFactory keyFactory = KeyFactory.getInstance("EC");
                        this.privateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
                }

                log.info("Apple App Store Server API initialized for environment: {}", environment);
        }

        /**
         * Apple Store Server API 호출을 위한 JWT 생성
         */
        private String createAppleJwt() {
                return JWT.create()
                                .withIssuer(issuerId)
                                .withIssuedAt(new Date())
                                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 5)) // 5분
                                .withAudience("appstoreconnect-v1")
                                .withClaim("bid", bundleId)
                                .withKeyId(keyId)
                                .sign(Algorithm.ECDSA256(null, privateKey));
        }

        /**
         * Transaction 정보 조회
         * 반환값은 JWS 서명된 문자열
         */
        public String getTransactionInfo(String transactionId) {
                return webClient.get()
                                .uri("/inApps/v1/transactions/{transactionId}", transactionId)
                                .header("Authorization", "Bearer " + createAppleJwt())
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(response -> (String) response.get("signedTransactionInfo"))
                                .block();
        }
}
