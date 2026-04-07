package com.loom.server;

import java.nio.file.Path;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class AbstractMySqlIntegrationTest {

    private static final Path TEST_SERVER_VAULT = Path.of("target", "test-vault");
    private static final Path TEST_LOCAL_VAULT = Path.of("target", "test-local-vault");
    private static final String TEST_DB = "loom_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + TEST_DB + ";MODE=MYSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("loom.bootstrap.enabled", () -> false);
        registry.add("loom.storage.server-vault-root", () -> TEST_SERVER_VAULT.toAbsolutePath().toString());
        registry.add("loom.storage.local-vault-root", () -> TEST_LOCAL_VAULT.toAbsolutePath().toString());
        registry.add("loom.nodes.server-token", () -> "test-token");
    }
}
