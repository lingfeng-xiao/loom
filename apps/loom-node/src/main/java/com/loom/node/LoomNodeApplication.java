package com.loom.node;

import com.loom.node.config.NodeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(NodeProperties.class)
public class LoomNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoomNodeApplication.class, args);
    }
}
