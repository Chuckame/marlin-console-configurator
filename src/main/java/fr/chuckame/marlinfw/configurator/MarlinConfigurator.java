package fr.chuckame.marlinfw.configurator;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;

public class MarlinConfigurator implements ApplicationRunner {
    public static void main(final String[] args) {
        SpringApplication.run(MarlinConfigurator.class, args);
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        System.err.println("hello");
    }
}
