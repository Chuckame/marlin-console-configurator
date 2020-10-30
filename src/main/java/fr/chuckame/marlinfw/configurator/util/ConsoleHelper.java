package fr.chuckame.marlinfw.configurator.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Scanner;
import java.util.function.Consumer;

@Service
public class ConsoleHelper {
    private static final String RESET_COLOR = "\u001B[0m";

    @RequiredArgsConstructor
    public enum ColorEnum {
        //  BLACK("\u001B[30m"), useless since it is the default background color
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        PURPLE("\u001B[35m"),
        CYAN("\u001B[36m"),
        //  WHITE("\u001B[37m") useless since it is the default color
        ;
        private final String colorCode;
    }

    public void writeLine(final String line, final ColorEnum color) {
        writeLine(String.join("", color.colorCode, line, RESET_COLOR));
    }

    public void writeLine(final String line) {
        System.out.println(line);
    }

    public void writeLine(final Consumer<StringBuilder> lineBuilder) {
        final var line = new StringBuilder();
        lineBuilder.accept(line);
        writeLine(lineBuilder.toString());
    }

    public void writeErrorLine(final String line) {
        System.err.println(line);
    }

    public String readLine() {
        return new Scanner(System.in).next();
    }
}
