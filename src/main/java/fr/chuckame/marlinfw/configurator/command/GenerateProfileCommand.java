package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.profile.ConstantHelper;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

@Component
@Parameters(commandNames = "generate-profile", commandDescription = "Generate a profile from given marlin constants files")
@RequiredArgsConstructor
public class GenerateProfileCommand implements Command {
    @Parameter(required = true, description = "/path1 /path2 ...\tThe marlin constants folder or files paths")
    private List<Path> filesPath;
    @Parameter(names = {"--diff-from"}, description = "The marlin constants folder or files paths from where you want to make a diff. If gathered, the generated profile will contains only the diff between those files and the command files")
    private List<Path> filesPathBase;
    @Parameter(names = {"--output", "-o"}, required = true, description = "The output profile path, will be overwritten if already existing file. If 'console' is specified, the profile will just be printed to the console")
    private Path profilePath;

    private static final Path CONSOLE_OUTPUT = Path.of("console");

    private final ProfilePropertiesParser profilePropertiesParser;
    private final ConstantHelper constantHelper;
    private final ConsoleHelper consoleHelper;

    @Override
    public Mono<Void> run() {
        final Flux<Constant> constants = CollectionUtils.isEmpty(filesPathBase) ? constantHelper.getConstants(filesPath) : getConstantsFromDiff();
        return constantHelper.constantsToProfile(constants)
                             .flatMap(profile -> profilePath.equals(CONSOLE_OUTPUT) ?
                                     profilePropertiesParser.writeToString(profile).doOnNext(consoleHelper::writeLine).then()
                                     : profilePropertiesParser.writeToFile(profile, profilePath));
    }

    /**
     * @return only constants that are not present from {@link #filesPathBase}, and only modified constants present on both sides
     */
    private Flux<Constant> getConstantsFromDiff() {
        return Mono.zip(constantHelper.getConstants(filesPathBase).collectMap(Constant::getName),
                        constantHelper.getConstants(filesPath).collectMap(Constant::getName))
                   .map(t -> Maps.difference(t.getT1(), t.getT2()))
                   .flatMapMany(diff -> Flux.fromIterable(diff.entriesDiffering().values())
                                            .map(MapDifference.ValueDifference::leftValue)
                                            .mergeWith(Flux.fromIterable(diff.entriesOnlyOnRight().values())));
    }
}
