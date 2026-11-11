package org.skylark.bluewhale.infrastructure.persistence;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class SqliteStoreConfiguration {

    @Bean
    public SQLiteDataSource dataSource(
            @Value("${bluewhale.store.sqlite.url:jdbc:sqlite:./data/bluewhale.db}") String url)
            throws IOException {
        prepareDirectory(url);
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5_000);
        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl(url);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(SQLiteDataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(SQLiteDataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private static void prepareDirectory(String url) throws IOException {
        String prefix = "jdbc:sqlite:";
        if (!url.startsWith(prefix)) {
            return;
        }
        String location = url.substring(prefix.length());
        if (location.isBlank() || location.equals(":memory:")
                || location.startsWith("file:")) {
            return;
        }
        Path absolute = Path.of(location).toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
