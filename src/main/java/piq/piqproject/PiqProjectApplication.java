package piq.piqproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableJpaAuditing // JPA Auditing 기능 활성화
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) //페이지네이션 응답과정에서 발생하는 불필요한 요소들을 제거해줍니다.
public class PiqProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiqProjectApplication.class, args);
    }

}
