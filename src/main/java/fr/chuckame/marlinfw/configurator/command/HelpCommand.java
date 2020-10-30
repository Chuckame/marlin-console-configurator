package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Parameters(commandNames = "help", commandDescription = "Display this help message")
@RequiredArgsConstructor
public class HelpCommand implements Command {
    private final ConsoleHelper consoleHelper;
    private final JCommander jCommander;

    @Override
    public Mono<Void> run() {
        return Mono.fromSupplier(StringBuilder::new)
                   .doOnNext(jCommander.getUsageFormatter()::usage)
                   .map(StringBuilder::toString)
                   .doOnNext(consoleHelper::writeLine)
                   .then();
    }
}
