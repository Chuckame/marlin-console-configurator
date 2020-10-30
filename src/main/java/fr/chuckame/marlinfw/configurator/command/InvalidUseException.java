package fr.chuckame.marlinfw.configurator.command;

import org.springframework.boot.ExitCodeGenerator;

public class InvalidUseException extends RuntimeException implements ExitCodeGenerator {
    public static final int EXIT_CODE = 2;

    public InvalidUseException(final String format, final Object... args) {
        super(String.format(format, args));
    }

    @Override
    public int getExitCode() {
        return EXIT_CODE;
    }
}
