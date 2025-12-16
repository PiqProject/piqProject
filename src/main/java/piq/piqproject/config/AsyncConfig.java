package piq.piqproject.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync // 비동기 처리 기능 활성화
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3); // 기본 스레드 수
        executor.setMaxPoolSize(10); // 최대 스레드 수
        executor.setQueueCapacity(50); // 대기 큐 크기
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();

        // 5. [중요] 예외 처리 정책: 대기열도 꽉 찼을 때 어떻게 할 것인가?
        // CallerRunsPolicy: 요청한 사람(Main Thread)이 직접 처리하게 함 (속도는 느려지지만 데이터 유실 없음)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}