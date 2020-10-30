package fr.chuckame.marlinfw.configurator.config;

import com.beust.jcommander.JCommander;
import fr.chuckame.marlinfw.configurator.command.Command;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JCommanderConfig {
    @Value("${command-usage}")
    private String commandUsage;

    @Bean
    public List<Command> commands(final ObjectProvider<List<Command>> commandsProvider) {
        final var commands = commandsProvider.getObject(jCommander());
        commands.forEach(jCommander()::addCommand);
        return commands;
    }

    @Bean
    public JCommander jCommander() {
        final var jcmd = new JCommander();
        jcmd.setProgramName(commandUsage);
        jcmd.setColumnSize(140);
        return jcmd;
    }
}
