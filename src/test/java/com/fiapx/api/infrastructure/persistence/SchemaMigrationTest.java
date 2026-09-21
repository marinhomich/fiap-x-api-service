package com.fiapx.api.infrastructure.persistence;

import com.fiapx.api.FiapXApiApplication;
import com.fiapx.api.config.TestRabbitConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a aplicação contra um PostgreSQL embarcado, aplica as migrações Flyway
 * e deixa o Hibernate validar (ddl-auto=validate) que o schema bate com as entidades.
 */
@ActiveProfiles("test")
@Import(TestRabbitConfig.class)
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
@SpringBootTest(classes = FiapXApiApplication.class, properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class SchemaMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrationsAndMatchJpaEntities() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");

        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN ('users', 'videos')",
                Integer.class);
        assertThat(tables).isEqualTo(2);
    }
}
