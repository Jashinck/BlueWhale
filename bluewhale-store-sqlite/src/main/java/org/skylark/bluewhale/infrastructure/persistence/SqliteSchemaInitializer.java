package org.skylark.bluewhale.infrastructure.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class SqliteSchemaInitializer {

    private static final List<String> APPEND_ONLY_TABLES = List.of(
            "bw_episode",
            "bw_lifecycle_event",
            "bw_usage_event",
            "bw_review",
            "bw_erasure",
            "bw_signal",
            "bw_attribution"
    );

    private final JdbcTemplate jdbc;

    public SqliteSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initialize() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/sqlite/schema.sql"));
        populator.execute(jdbc.getDataSource());
        for (String table : APPEND_ONLY_TABLES) {
            jdbc.execute("""
                    CREATE TRIGGER IF NOT EXISTS %1$s_no_update
                    BEFORE UPDATE ON %1$s
                    BEGIN
                        SELECT RAISE(ABORT, 'append-only table');
                    END
                    """.formatted(table));
            jdbc.execute("""
                    CREATE TRIGGER IF NOT EXISTS %1$s_no_delete
                    BEFORE DELETE ON %1$s
                    BEGIN
                        SELECT RAISE(ABORT, 'append-only table');
                    END
                    """.formatted(table));
        }
    }
}
