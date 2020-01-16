package com.inventorsoft.demo2;

import com.inventorsoft.demo2.domain.model.Item;
import com.inventorsoft.demo2.domain.repository.ItemRepository;
import com.inventorsoft.demo2.domain.service.ItemService;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JobStarter {

    Resource resource = new ClassPathResource("demo2.csv");

    ItemRepository itemRepository;
    ItemService itemService;

    ApplicationContext applicationContext;
    DataSource dataSource;

    public JobStarter(JdbcTemplate jdbcTemplate,
                      ItemRepository itemRepository,
                      DataSource dataSource,
                      ItemService itemService,
                      ApplicationContext applicationContext) {

        this.itemRepository = itemRepository;
        this.dataSource = dataSource;
        this.itemService = itemService;
        this.applicationContext = applicationContext;

        createSchema(jdbcTemplate);
        generateFile();
    }

    @Bean
    Step step(StepBuilderFactory stepBuilderFactory, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        return stepBuilderFactory
                .get("demo-2-step")
                .<Integer, Item>chunk(1)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .listener(itemWriteListener())
                .taskExecutor(threadPoolTaskExecutor)
                .throttleLimit(threadPoolTaskExecutor.getCorePoolSize())
                .build();
    }

    @Bean
    Job job(JobBuilderFactory jobBuilderFactory, Step step) {
        return jobBuilderFactory
                .get("demo-2-job")
                .start(step)
                .listener(jobExecutionListener())
                .build();
    }

    @Bean
    ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        int threads = 3;

        threadPoolTaskExecutor.setCorePoolSize(threads);
        threadPoolTaskExecutor.setMaxPoolSize(threads);
        threadPoolTaskExecutor.setThreadNamePrefix("custom-pool-");

        return threadPoolTaskExecutor;
    }

    @SneakyThrows
    private void generateFile() {
        File file = resource.getFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

            IntStream
                    .range(0, 100)
                    .boxed()
                    .peek(number -> writeToFile(bufferedWriter, number))
                    .forEach(__ -> addNewLine(bufferedWriter));

            bufferedWriter.flush();
        }
    }

    private void createSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE items (
                    id bigserial not null primary key,
                    original_value integer,
                    modified_value integer
                )
                """);
    }

    @SneakyThrows
    private void writeToFile(BufferedWriter writer, Integer number) {
        writer.write(number.toString());
    }

    @SneakyThrows
    private void addNewLine(BufferedWriter writer) {
        writer.newLine();
    }

    private ItemWriteListener<Item> itemWriteListener() {
        return new ItemWriteListener<>() {

            @Override
            @SneakyThrows
            public void beforeWrite(List<? extends Item> items) {
                TimeUnit.MILLISECONDS.sleep(400L);
            }

            @Override
            public void afterWrite(List<? extends Item> items) {

            }

            @Override
            public void onWriteError(Exception exception, List<? extends Item> items) {

            }
        };
    }

    private ItemWriter<Item> writer() {
        /*ItemPreparedStatementSetter<Item> preparedStatementSetter = (item, ps) -> {
            ps.setInt(1, item.getOriginalValue());
            ps.setInt(2, item.getModifiedValue());
        };

        return new JdbcBatchItemWriterBuilder<Item>()
                .sql("INSERT INTO items (original_value, modified_value) VALUES(?, ?)")
                .itemPreparedStatementSetter(preparedStatementSetter)
                .dataSource(dataSource)
                .build();*/

        return new RepositoryItemWriterBuilder<Item>()
                .repository(itemRepository)
                .methodName("save")
                .build();
    }

    private ItemProcessor<Integer, Item> processor() {
        return number -> {
            Item item = new Item();
            item.setOriginalValue(number);
            item.setModifiedValue(number * number);
            return item;
        };
    }

    private ItemReader<Integer> reader() {
        return new FlatFileItemReaderBuilder<Integer>()
                .lineTokenizer(new DelimitedLineTokenizer())
                .fieldSetMapper(fieldSet -> fieldSet.readInt(0))
                .name("demo-2-reader")
                .resource(resource)
                .build();
    }

    private JobExecutionListener jobExecutionListener() {
        return new JobExecutionListenerSupport() {
            @Override
            public void afterJob(JobExecution jobExecution) {
                List<Item> all = itemService.findAll();
                if (100 != all.size()) {
                    throw new RuntimeException("Error");
                }
                SpringApplication.exit(applicationContext);
            }
        };
    }

}
