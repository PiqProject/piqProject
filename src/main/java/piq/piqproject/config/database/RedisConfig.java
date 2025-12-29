package piq.piqproject.config.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
// Redis 리포지토리가 있는 패키지만 딱 집어서 설정
@EnableRedisRepositories(basePackages = "piq.piqproject.domain.auth.repository")
public class RedisConfig {
    // Redis ConnectionFactory 등 기존 빈 설정이 있다면 여기에 포함
}