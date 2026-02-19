package piq.piqproject.infra.external.google.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import org.springframework.core.io.ClassPathResource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Slf4j
@Service
public class GooglePlayClientService {

    @Value("${google.play.package-name}")
    private String packageName;

    @Value("${google.play.service-account-key-path}")
    private String serviceAccountKeyPath;

    private AndroidPublisher androidPublisher;

    @PostConstruct
    public void init() throws Exception {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // 1. ClassPathResource로 파일 찾기 (src/main/resources 기준)
            ClassPathResource resource = new ClassPathResource(serviceAccountKeyPath);

            if (!resource.exists()) {
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "구글 서비스 계정 키 파일을 찾을 수 없습니다: " + serviceAccountKeyPath);
            }

            // 2. InputStream으로 읽어서 GoogleCredentials 생성
            // (FileInputStream 대신 resource.getInputStream() 사용)
            GoogleCredentials credentials;
            try (InputStream inputStream = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(inputStream)
                        .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));
            }

            // 3. AndroidPublisher 생성
            androidPublisher = new AndroidPublisher.Builder(httpTransport, jsonFactory,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(packageName)
                    .build();

            log.info("Google Play Developer API initialized for package: {}", packageName);

        } catch (IOException e) {
            log.error("구글 서비스 계정 키 로드 실패", e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "구글 API 초기화 실패");
        }
    }

    /**
     * 구매 정보 상세 조회
     */
    public ProductPurchase getProductPurchase(String productId, String token) {
        try {
            return androidPublisher.purchases().products().get(packageName, productId, token).execute();
        } catch (IOException e) {
            log.error("Google Play API 조회 실패: productId={}, token={}", productId, token, e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "Google Play API 연동 오류");
        }
    }

    /**
     * 구매 승인 (Acknowledge)
     */
    public void acknowledgePurchase(String productId, String token) {
        try {
            androidPublisher.purchases().products().acknowledge(packageName, productId, token, null).execute();
            log.info("Google Play 구매 승인 완료: productId={}, token={}", productId, token);
        } catch (IOException e) {
            log.error("Google Play API 승인 실패: productId={}, token={}", productId, token, e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "Google Play API 승인 오류");
        }
    }
}
