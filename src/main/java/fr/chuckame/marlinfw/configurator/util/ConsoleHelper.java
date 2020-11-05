package fr.chuckame.marlinfw.configurator.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConsoleHelper {
    private static final String RESET_COLOR = "\u001B[0m";


    public interface ConsoleStyle {
        String getCode();
    }

    @RequiredArgsConstructor
    public enum ForegroundColorEnum implements ConsoleStyle {
        DEFAULT("\u001B[39m"),
        BLACK("\u001B[30m"),
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        MAGENTA("\u001B[35m"),
        CYAN("\u001B[36m"),
        LIGHT_GRAY("\u001B[37m"),
        DARK_GRAY("\u001B[90m"),
        LIGHT_RED("\u001B[91m"),
        LIGHT_GREEN("\u001B[92m"),
        LIGHT_YELLOW("\u001B[93m"),
        LIGHT_BLUE("\u001B[94m"),
        LIGHT_MAGENTA("\u001B[95m"),
        LIGHT_CYAN("\u001B[96m"),
        ;
        @Getter
        private final String code;
    }

    @RequiredArgsConstructor
    public enum FormatterEnum implements ConsoleStyle {
        BOLD("\u001B[1m"),
        UNDERLINED("\u001B[4m"),
        DIM("\u001B[2m"),
        ;
        @Getter
        private final String code;
    }

    public void writeLine(final String line, final ConsoleStyle... styles) {
        System.out.println(String.join("", Stream.of(styles).map(ConsoleStyle::getCode).collect(Collectors.joining("")), line, RESET_COLOR));
    }

    public void newLine() {
        System.out.println();
    }

    public void writeLine(final Consumer<StringBuilder> lineBuilder, final ConsoleStyle... styles) {
        final var line = new StringBuilder();
        lineBuilder.accept(line);
        writeLine(line.toString(), styles);
    }

    public void writeErrorLine(final String line) {
        System.err.println(line);
    }

    public String readLine() {
        return new Scanner(System.in).next();
    }
}
