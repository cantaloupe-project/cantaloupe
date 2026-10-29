package edu.illinois.library.cantaloupe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot main application class for Cantaloupe.
 * Replaces the previous StandaloneEntry class.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class Cantaloupe {

    public static void main(String[] args) {
        SpringApplication.run(Cantaloupe.class, args);
    }
}
