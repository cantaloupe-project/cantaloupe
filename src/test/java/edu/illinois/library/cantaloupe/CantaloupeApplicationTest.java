package edu.illinois.library.cantaloupe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic Spring Boot application test to verify the application context loads correctly.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class CantaloupeApplicationTest {

    @Test
    void contextLoads() {
        // This test will fail if the Spring Boot application context cannot be loaded
        // It's a basic smoke test to ensure the migration is working
    }
}
