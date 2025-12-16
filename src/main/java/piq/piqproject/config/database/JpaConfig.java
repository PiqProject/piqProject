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
                basePackages = "piq.piqproject.domain",

                // 2. (중요) Elasticsearch용 리포지토리는 JPA가 건드리면 안 되므로 제외(exclude)합니다.
                // 우리가 앞서 만든 ElasticsearchConfig에서 지정한 패키지 경로입니다.
                excludeFilters = {
                                @Filter(type = FilterType.REGEX, pattern = "piq\\.piqproject\\.domain\\.search\\.repository\\.es\\..*"),
                                @Filter(type = FilterType.REGEX, pattern = "piq\\.piqproject\\.domain\\.auth\\.repository\\..*")
                })
public class JpaConfig {
        // 별도의 내용이 없어도 @EnableJpaRepositories 어노테이션만으로 충분합니다.
}