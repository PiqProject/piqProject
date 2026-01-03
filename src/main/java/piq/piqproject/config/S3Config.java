package piq.piqproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${cloud.s3.access-key}")
    private String accessKey;

    @Value("${cloud.s3.secret-key}")
    private String secretKey;

    @Value("${cloud.s3.endpoint}")
    private String endpoint;

    @Value("${cloud.s3.region}")
    private String region;

    // S3Client Bean을 프로그램 실행시점에 만들어둠
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                // [중요] AWS가 아닌 가비아 서버 주소로 강제 변경
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}