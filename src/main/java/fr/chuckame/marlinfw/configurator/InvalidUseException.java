package fr.chuckame.marlinfw.configurator;

public class InvalidUseException extends RuntimeException {
    public InvalidUseException(final String format, final Object... args) {
        super(String.format(format, args));
    }
}
