package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import fr.chuckame.marlinfw.configurator.InvalidUseException;
import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeFormatter;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ProfilePropertiesChangeAdapter;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Parameters(commandNames = "apply-profile")
@RequiredArgsConstructor
public class ApplyProfileCommand implements Command {
    @Parameter(names = {"--profile", "-p"}, required = true)
    private Path profilePath;
    @Parameter(names = {"--file", "--files", "-f"}, required = true)
    private List<Path> filesPath;
    @Parameter(names = {"--save", "-s"})
    private boolean doSave;
    @Parameter(names = {"--yes", "-y"})
    private boolean applyWithoutPrompt;

    private final ProfilePropertiesChangeAdapter changeAdapter;
    private final LineChangeManager lineChangeManager;
    private final LineChangeFormatter lineChangeFormatter;
    private final ProfilePropertiesParser profilePropertiesParser;
    private final FileHelper fileHelper;

    @Override
    public Mono<Void> run() {
        return profilePropertiesParser.parseFromFile(profilePath)
                                      .map(changeAdapter::getWantedConstants)
                                      .flatMap(wantedConstants ->
                                                       prepareChanges(filesPath, wantedConstants)
                                                               .flatMap(changes -> printChanges(changes)
                                                                       .then(printUnusedConstants(changes, wantedConstants))
                                                                       .then(doSave ? checkIfUserAgree().then(applyAndSaveChanges(changes)) : Mono.empty()))
                                      );
    }

    private Mono<Void> printChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .concatMap(fileChanges -> Flux.concat(
                           Flux.just(String.format("%s change(s) for file %s:", fileChanges.getValue().size(), fileChanges.getKey())),
                           Flux.fromIterable(fileChanges.getValue()).filter(LineChange::isConstant).map(lineChangeFormatter::format),
                           Flux.just("")
                   ))
                   .doOnNext(System.out::println)
                   .then()
                ;
    }

    private Mono<Void> printUnusedConstants(final Map<Path, List<LineChange>> changes, final Map<String, Constant> wantedConstants) {
        return lineChangeManager.getUnusedWantedConstants(changes.values().stream().flatMap(List::stream).collect(Collectors.toList()), wantedConstants)
                                .collectList()
                                .filter(Predicate.not(List::isEmpty))
                                .doOnNext(unusedConstants -> System.out.printf("Still some unused constants: %s%n", unusedConstants))
                                .then();
    }

    private Mono<Void> checkIfUserAgree() {
        if (applyWithoutPrompt) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> System.out.println("Apply changes ? type 'y' to apply changes, or everything else to cancel"))
                   .then(Mono.fromSupplier(() -> new Scanner(System.in).next()))
                   .filter("y"::equals)
                   .switchIfEmpty(Mono.error(() -> new InvalidUseException("User refused to apply")))
                   .then();
    }

    public Mono<Map<Path, List<LineChange>>> prepareChanges(final List<Path> filesPath, final Map<String, Constant> wantedConstants) {
        return Flux.fromIterable(filesPath)
                   .flatMap(filePath -> fileHelper.lines(filePath)
                                                  .index()
                                                  .concatMap(line -> lineChangeManager.prepareChange(line.getT2(), line.getT1().intValue(), wantedConstants))
                                                  .collectList()
                                                  .zipWith(Mono.just(filePath)))
                   .collectMap(Tuple2::getT2, Tuple2::getT1);
    }

    public Mono<Void> applyAndSaveChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .groupBy(Map.Entry::getKey, Map.Entry::getValue)
                   .flatMap(fileChanges -> fileHelper.write(fileChanges.key(), true, fileChanges.flatMap(this::applyChanges)))
                   .then();
    }

    public Flux<String> applyChanges(final Collection<LineChange> changes) {
        return Flux.fromIterable(changes).flatMap(lineChangeManager::applyChange);
    }
}
