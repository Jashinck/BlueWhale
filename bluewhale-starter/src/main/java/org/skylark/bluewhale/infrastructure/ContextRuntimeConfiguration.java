package org.skylark.bluewhale.infrastructure;

import org.skylark.bluewhale.domain.context.AccessPolicy;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ConnectorPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.skylark.bluewhale.domain.port.ReleasePort;
import org.skylark.bluewhale.domain.port.WikiStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableAsync
public class ContextRuntimeConfiguration {

    @Bean("usageExecutor")
    public ThreadPoolTaskExecutor usageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("bluewhale-usage-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ClockPort clockPort() {
        return Instant::now;
    }

    @Bean
    public ReleasePort releasePort(@Value("${bluewhale.release-id:local}") String releaseId) {
        return () -> releaseId;
    }

    @Bean
    public OntologyPort ontologyPort(
            @Value("${bluewhale.ontology.location:ontology/default.yaml}") String location) {
        return new YamlOntologyAdapter(location);
    }

    @Bean
    public ConnectorPort connectorPort() {
        return new FilesystemConnectorAdapter();
    }

    @Bean
    public WikiStorePort wikiStorePort(
            @Value("${bluewhale.wiki.repo-dir:./data/wiki}") String repoDir) {
        return new GitWikiStoreAdapter(java.nio.file.Path.of(repoDir));
    }

    @Bean
    @ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
    public PlatformTransactionManager memoryTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }

    @Bean
    public AccessPolicy accessPolicy(
            @Value("${bluewhale.access.allowed-tenants:}") String tenants,
            @Value("${bluewhale.access.allowed-workspaces:}") String workspaces,
            @Value("${bluewhale.access.allowed-agents:}") String agents) {
        return new AccessPolicy(values(tenants), values(workspaces), values(agents));
    }

    private static Set<String> values(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
