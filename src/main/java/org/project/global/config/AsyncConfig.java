package org.project.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("ioExecutor")
    public Executor ioExecutor() {
        int cores = Runtime.getRuntime().availableProcessors(); // 현재 실행 중인 환경의 CPU 코어 수를 가져 옴
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores * 2); //  평상시 유지하는 스레드
        executor.setMaxPoolSize(cores * 4); //  요청이 몰릴 때 최대로 늘릴 수 있는 스레드 수
        executor.setQueueCapacity(100); // 대기시킬 수 있는 작업 수
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 큐 포화 시 예외 대신 요청 스레드가 직접 실행 (500 에러 방지)
        executor.setWaitForTasksToCompleteOnShutdown(true); // 재배포 시 진행 중인 비동기 작업 완료 후 종료
        executor.setAwaitTerminationSeconds(30); // 최대 30초 대기, 이후 강제 종료
        executor.setThreadNamePrefix("io-");
        executor.initialize();
        return executor;
    }
}

