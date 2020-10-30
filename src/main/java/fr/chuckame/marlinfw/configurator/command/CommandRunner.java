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
    public void run(final String... args) throws Exception {
        try {
            jCommander.parse(args);
        } catch (final ParameterException e) {
            consoleHelper.writeErrorLine("Bad argument: " + e.getMessage());
            consoleHelper.writeLine(jCommander.getUsageFormatter()::usage);
            System.exit(InvalidUseException.EXIT_CODE);
        }
        try {
            final Command command = (Command) jCommander.findCommandByAlias(jCommander.getParsedAlias()).getObjects().get(0);
            command.run().blockOptional();
        } catch (final ManuallyStoppedException e) {
            consoleHelper.writeErrorLine(e.getMessage());
            System.exit(e.getExitCode());
        }
    }
}
