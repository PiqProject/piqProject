package piq.piqproject.config.webconfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // LocalUploader에서 설정한 실제 파일 저장 경로를 여기에 동일하게 적어줍니다.
    private final String uploadDir = "C:/uploads/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**") // 1. 웹 브라우저에 입력할 URL
                .addResourceLocations("file:" + uploadDir); // 2. 서버에 저장된 실제 폴더 경로
    }
}