package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.chuckame.marlinfw.configurator.InvalidUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandRunner implements CommandLineRunner {
    private final JCommander jCommander;

    @Override
    public void run(final String... args) throws Exception {
        try {
            jCommander.parse(args);
        } catch (final ParameterException e) {
            System.err.println("Bad argument: " + e.getMessage());
            e.usage();
            System.exit(1);
        }
        try {
            final Command command = (Command) jCommander.findCommandByAlias(jCommander.getParsedAlias()).getObjects().get(0);
            command.run().blockOptional();
        } catch (final InvalidUseException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }

    }
}
