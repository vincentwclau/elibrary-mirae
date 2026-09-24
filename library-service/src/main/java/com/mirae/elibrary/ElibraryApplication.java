package com.mirae.elibrary;

import com.mirae.elibrary.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ElibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElibraryApplication.class, args);
    }

    /** System clock; injected into services so time-based logic stays testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
