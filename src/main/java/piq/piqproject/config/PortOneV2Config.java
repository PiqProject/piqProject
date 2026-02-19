package piq.piqproject.config;

import io.portone.sdk.server.PortOneClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortOneV2Config {

    @Value("${portone.v2.secret}")
    private String apiSecret;

    @Value("${portone.v2.storeId}")
    private String storeId;

    @Bean
    public PortOneClient portOneClient() {
        return new PortOneClient(apiSecret, "https://api.portone.io", storeId);
    }
}