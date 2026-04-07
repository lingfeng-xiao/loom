package com.template.server;

import com.template.server.config.TemplateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TemplateProperties.class)
public class TemplateServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateServerApplication.class, args);
    }
}
