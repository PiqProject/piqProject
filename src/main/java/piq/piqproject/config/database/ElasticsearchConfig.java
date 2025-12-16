package piq.piqproject.config.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
// ▼ [중요] Elasticsearch Repository가 위치할 패키지를 명시적으로 지정합니다.
// 나중에 우리가 'domain.search.repository' 패키지를 만들어서 거기에 ES용 리포지토리를 넣을 것입니다.
// 이렇게 해야 JPA Repository와 충돌이 나지 않습니다.
@EnableElasticsearchRepositories(basePackages = "piq.piqproject.domain.search.repository.es")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("localhost:9200") // 도커 컨테이너 주소
                .withConnectTimeout(10000) // 연결 타임아웃 10초
                .withSocketTimeout(10000) // 소켓(읽기) 타임아웃 10초
                .build();
    }
}