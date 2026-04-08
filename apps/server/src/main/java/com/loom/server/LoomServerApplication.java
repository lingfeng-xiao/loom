package com.loom.server;

import com.loom.server.config.LoomServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LoomServerProperties.class)
public class LoomServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoomServerApplication.class, args);
    }
}
