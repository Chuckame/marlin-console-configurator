package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
