package fr.chuckame.marlinfw.configurator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarlinConfigurator {
    public static void main(final String[] args) {
        SpringApplication.run(MarlinConfigurator.class, "apply-profiled", "--save", "--profile", "src/test/resources/profile.yaml", "--file", "src/test/resources/file.h");
        //SpringApplication.run(MarlinConfigurator.class, "generate-profile", "--input", "src/test/resources/file.h", "--output", "src/test/resources/profile.yaml");
    }
}

