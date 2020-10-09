package fr.chuckame.marlinfw.configurator.change;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BadLineChangeException extends RuntimeException {
    private final String message;
    private final LineChange lineChange;

    @Override
    public String getMessage() {
        return String.format("Line[%s]: %s", lineChange.getLineNumber(), message);
    }
}
