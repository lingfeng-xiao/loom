package com.loom.node.config;

import com.loom.node.client.NodeServerClient;
import com.loom.node.state.NodeStateStore;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class NodeRuntimeConfig {

    @Bean
    public RestTemplate nodeRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public NodeStateStore nodeStateStore(NodeProperties properties) {
        return new NodeStateStore(properties);
    }
}
