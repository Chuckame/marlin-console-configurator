package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandRunner implements CommandLineRunner {
    private final JCommander jCommander;
    private final ConsoleHelper consoleHelper;

    @Override
    public void run(final String[] args) throws Exception {
        try {
            findCommandByAlias(parseAlias(args)).run().blockOptional();
        } catch (final ManuallyStoppedException e) {
            consoleHelper.writeErrorLine(e.getMessage());
            System.exit(e.getExitCode());
        } catch (final Exception e) {
            if (e.getCause() instanceof NoSuchFileException) {
                consoleHelper.writeErrorLine("File not found: " + Path.of(e.getCause().getMessage()).toAbsolutePath());
                System.exit(4);
            } else if (e.getCause() instanceof FileAlreadyExistsException) {
                consoleHelper.writeErrorLine("File already present: " + Path.of(e.getCause().getMessage()).toAbsolutePath());
                System.exit(6);
            } else {
                consoleHelper.writeErrorLine(e.getMessage());
                System.exit(5);
            }
        }
    }

    private String parseAlias(final String[] args) {
        String errorMessage = null;
        try {
            jCommander.parse(args);
        } catch (final ParameterException e) {
            errorMessage = e.getMessage();
        }
        if (errorMessage == null && jCommander.getParsedAlias() != null) {
            return jCommander.getParsedAlias();
        }

        if (errorMessage == null) {
            consoleHelper.writeErrorLine("No argument passed");
        } else {
            consoleHelper.writeErrorLine("Bad argument: " + errorMessage);
        }
        consoleHelper.writeLine(jCommander.getUsageFormatter()::usage);
        System.exit(InvalidUseException.EXIT_CODE);
        return null;
    }

    private Command findCommandByAlias(final String alias) {
        return (Command) jCommander.findCommandByAlias(alias).getObjects().get(0);
    }
}
