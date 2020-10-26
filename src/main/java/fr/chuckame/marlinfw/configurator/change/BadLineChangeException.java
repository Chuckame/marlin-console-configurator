package fr.chuckame.marlinfw.configurator.change;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BadLineChangeException extends RuntimeException {
    private final String message;
    @Getter
    private final LineChange lineChange;

    @Override
    public String getMessage() {
        return String.format("Line[%s]: %s", lineChange.getLineNumber(), message);
    }
}
