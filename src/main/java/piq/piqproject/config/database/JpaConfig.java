package piq.piqproject.config.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

@Configuration
@EnableJpaRepositories(
                // 1. JPA Repository가 위치한 패키지를 지정합니다.
                // 우리는 domain 패키지 하위에 기능별(users, matches 등)로 흩어져 있으므로
                // 상위 패키지인 'piq.piqproject.domain'을 지정합니다.
                basePackages = "piq.piqproject.domain", excludeFilters = {
                                @Filter(type = FilterType.REGEX, pattern = "piq\\.piqproject\\.domain\\.auth\\.repository\\..*")
                })
public class JpaConfig {
}