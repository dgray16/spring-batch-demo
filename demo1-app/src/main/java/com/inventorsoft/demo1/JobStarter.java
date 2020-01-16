package com.inventorsoft.demo1;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JobStarter {

    Map<Integer, String> map = new ConcurrentHashMap<>(100);

    @Bean
    Step step(StepBuilderFactory stepBuilderFactory, ThreadPoolTaskExecutor threadPoolTaskExecutor) {

        List<Integer> integers = Collections.synchronizedList(
                IntStream.range(0, 100).boxed().collect(Collectors.toList())
        );

        ItemReader<Integer> reader = () -> integers.isEmpty() ? null : integers.remove(0);

        ItemWriter<Integer> writer = items -> {
            TimeUnit.MILLISECONDS.sleep(400L);
            if (Objects.nonNull(items) && !items.isEmpty()) {

                Map<Integer, String> newMap = items.stream()
                        .collect(Collectors.toMap(Function.identity(), String::valueOf));

                map.putAll(newMap);

                log.info("Map size: {}", map.size());
            }
        };

        return stepBuilderFactory
                .get("demo-1-step")
                .<Integer, Integer>chunk(1)
                .reader(reader)
                .writer(writer)
                .taskExecutor(threadPoolTaskExecutor)
                .throttleLimit(threadPoolTaskExecutor.getCorePoolSize())
                .build();
    }

    @Bean
    Job job(JobBuilderFactory jobBuilderFactory, Step step, ApplicationContext applicationContext) {
        return jobBuilderFactory
                .get("demo-1-job")
                .start(step)
                .listener(new JobExecutionListenerSupport() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        SpringApplication.exit(applicationContext);
                    }
                })
                .build();
    }

    @Bean
    ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        int threads = 1;

        threadPoolTaskExecutor.setCorePoolSize(threads);
        threadPoolTaskExecutor.setMaxPoolSize(threads);
        threadPoolTaskExecutor.setThreadNamePrefix("custom-pool-");

        return threadPoolTaskExecutor;
    }

}
