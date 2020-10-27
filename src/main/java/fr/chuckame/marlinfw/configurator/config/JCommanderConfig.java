package fr.chuckame.marlinfw.configurator.config;

import com.beust.jcommander.JCommander;
import fr.chuckame.marlinfw.configurator.command.Command;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JCommanderConfig {
    @Bean
    public JCommander jCommander(final List<Command> commands, @Value("${command-usage}") final String commandUsage) {
        final var jcmd = JCommander.newBuilder();
        jcmd.programName(commandUsage);
        commands.forEach(jcmd::addCommand);
        return jcmd.build();
    }
}
