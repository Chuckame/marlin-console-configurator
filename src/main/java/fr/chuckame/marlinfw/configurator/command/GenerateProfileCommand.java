package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import fr.chuckame.marlinfw.configurator.profile.ConstantHelper;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

@Component
@Parameters(commandNames = "generate-profile")
@RequiredArgsConstructor
public class GenerateProfileCommand implements Command {
    @Parameter(names = {"--output", "-o"}, required = true)
    private Path profilePath;
    @Parameter(names = {"--input", "-i"}, required = true)
    private List<Path> filesPath;

    private final ProfilePropertiesParser profilePropertiesParser;
    private final FileHelper fileHelper;
    private final ConstantHelper constantHelper;
    private final ConstantLineInterpreter constantLineInterpreter;

    // todo: without --output, print to console

    @Override
    public Mono<Void> run() {
        return constantHelper.constantsToProfile(Flux.fromIterable(filesPath)
                                                     .flatMap(fileHelper::lines)
                                                     .flatMap(constantLineInterpreter::parseLine)
                                                     .map(ConstantLineInterpreter.ParsedConstant::getConstant))
                             .flatMap(profile -> profilePropertiesParser.writeToFile(profile, profilePath));
    }
}
