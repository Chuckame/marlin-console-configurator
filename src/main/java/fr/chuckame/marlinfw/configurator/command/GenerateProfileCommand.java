package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import fr.chuckame.marlinfw.configurator.profile.ConstantHelper;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

@Component
@Parameters(commandNames = "generate-profile", commandDescription = "Generate a profile from given marlin constants files")
@RequiredArgsConstructor
public class GenerateProfileCommand implements Command {
    @Parameter(required = true, description = "/path1 /path2 ...\tThe marlin constants folder or files paths")
    private List<Path> filesPath;
    @Parameter(names = {"--output", "-o"}, required = true, description = "The output profile path, will be overwritten if already existing file. If 'console' is specified, the profile will just be printed to the console")
    private Path profilePath;

    private static final Path CONSOLE_OUTPUT = Path.of("console");

    private final ProfilePropertiesParser profilePropertiesParser;
    private final FileHelper fileHelper;
    private final ConstantHelper constantHelper;
    private final ConstantLineInterpreter constantLineInterpreter;
    private final ConsoleHelper consoleHelper;

    @Override
    public Mono<Void> run() {
        return constantHelper.constantsToProfile(fileHelper.listFiles(filesPath)
                                                           .flatMap(fileHelper::lines)
                                                           .flatMap(constantLineInterpreter::parseLine)
                                                           .map(ConstantLineInterpreter.ParsedConstant::getConstant))
                             .flatMap(profile -> profilePath.equals(CONSOLE_OUTPUT) ?
                                     profilePropertiesParser.writeToString(profile).doOnNext(consoleHelper::writeLine).then()
                                     : profilePropertiesParser.writeToFile(profile, profilePath));
    }
}
