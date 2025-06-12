package com.aadimngmnt.config;

import com.aadimngmnt.Collection.Invoice;
import com.aadimngmnt.service.Processor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MongoTemplate mongoTemplate;
    private Processor processor;

    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager, MongoTemplate mongoTemplate, Processor processor) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.mongoTemplate = mongoTemplate;
        this.processor = processor;
    }

    @Bean
    public FlatFileItemReader<Invoice> csvReader() {
        FlatFileItemReader<Invoice> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("invoice_data.csv"));
        reader.setLinesToSkip(1); // Skip CSV header

        DefaultLineMapper<Invoice> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(
                "uploadCode", "entryCode", "entryType", "ledgerCode",
                "clientCode", "clientName", "date", "securityCode",
                "amount", "cgst", "sgst", "igst", "total",
                "ledgerName", "dc", "narration", "recordNumber"
        );

        BeanWrapperFieldSetMapper<Invoice> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Invoice.class);

        // ✅ Register custom property editor for LocalDate
        Map<Class<?>, PropertyEditor> customEditors = new HashMap<>();
        customEditors.put(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        });
        mapper.setCustomEditors(customEditors);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        reader.setLineMapper(lineMapper);
        return reader;
    }


    // Mongo writer
    @Bean
    public MongoItemWriter<Invoice> writer() {
        MongoItemWriter<Invoice> writer = new MongoItemWriter<>();
        writer.setTemplate(mongoTemplate);
        writer.setCollection("invoices");
        return writer;
    }

        @Bean
        public Step csvToMongoStep(FlatFileItemReader<Invoice> reader,
                                   ItemProcessor<Invoice, Invoice> processor,
                                   MongoItemWriter<Invoice> writer) {

            return new StepBuilder("csvToMongoStep", jobRepository)
                    .<Invoice, Invoice>chunk(5, transactionManager)
                    .reader(reader)
                    .processor(processor)
                    .writer(writer)
                    .build();
        }

        @Bean
        public Job csvToMongoJob(Step csvToMongoStep) {
            return new JobBuilder("csvToMongoJob", jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .start(csvToMongoStep)
                    .build();
        }






}
