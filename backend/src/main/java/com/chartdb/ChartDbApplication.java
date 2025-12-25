package com.chartdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
@EnableJpaAuditing
@EnableScheduling
public class ChartDbApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ChartDbApplication.class, args);
    }
}
