package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeFormatter;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ProfilePropertiesChangeAdapter;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Parameters(commandNames = "apply", commandDescription = "Apply the given profile to marlin constants files, that will enable, change value or disable constants into marlin configuration files")
@RequiredArgsConstructor
public class ApplyCommand implements Command {
    @Parameter(required = true, description = "/path1 /path2 ...\tFile(s) path(s) where all changes will be applied")
    private List<Path> filesPath;
    @Parameter(names = {"--profile", "-p"}, required = true, description = "Profile's path containing changes to apply. Format: yaml")
    private Path profilePath;
    @Parameter(names = {"--save", "-s"}, description = "When is present, will save changes to files. Else, just display changes without saving")
    private boolean doSave;
    @Parameter(names = {"--yes", "-y"}, description = "when present, the changes will be saved without prompting the user")
    private boolean applyWithoutPrompt;

    private final ProfilePropertiesChangeAdapter changeAdapter;
    private final LineChangeManager lineChangeManager;
    private final LineChangeFormatter lineChangeFormatter;
    private final ProfilePropertiesParser profilePropertiesParser;
    private final FileHelper fileHelper;
    private final ConsoleHelper consoleHelper;

    @Override
    public Mono<Void> run() {
        return profilePropertiesParser.parseFromFile(profilePath)
                                      .map(changeAdapter::profileToConstants)
                                      .flatMap(wantedConstants ->
                                                       prepareChanges(wantedConstants)
                                                               .flatMap(changes -> printChanges(changes)
                                                                       .then(printUnusedConstants(changes, wantedConstants))
                                                                       .then(doSave ? checkIfUserAgree().then(applyAndSaveChanges(changes)) : Mono.empty()))
                                      );
    }

    public Mono<Map<Path, List<LineChange>>> prepareChanges(final Map<String, Constant> wantedConstants) {
        return fileHelper.listFiles(filesPath)
                         .flatMap(filePath -> fileHelper.lines(filePath)
                                                        .index()
                                                        .concatMap(line -> lineChangeManager.prepareChange(line.getT2(), line.getT1().intValue(), wantedConstants))
                                                        .collectList()
                                                        .filter(changes -> changes.stream().anyMatch(LineChange::isConstant))
                                                        .map(changes -> Tuples.of(filePath, changes)))
                         .collectMap(Tuple2::getT1, Tuple2::getT2);
    }

    private Mono<Void> printChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .concatMap(fileChanges -> Flux.concat(
                           Mono.fromRunnable(() -> consoleHelper
                                   .writeLine(String.format("%s change(s) to apply for file %s:", fileChanges.getValue().stream().filter(this::isModifyingChange)
                                                                                                             .count(), fileChanges
                                                                    .getKey()))),
                           Flux.fromIterable(fileChanges.getValue()).filter(LineChange::isConstant).doOnNext(change -> consoleHelper
                                   .writeLine(lineChangeFormatter.format(change), change
                                           .getDiff() == LineChange.DiffEnum.DO_NOTHING ? ConsoleHelper.ColorEnum.DEFAULT : ConsoleHelper.ColorEnum.YELLOW)),
                           Mono.fromRunnable(() -> consoleHelper.writeLine(""))
                   ))
                   .then()
                ;
    }

    private Mono<Void> printUnusedConstants(final Map<Path, List<LineChange>> changes, final Map<String, Constant> wantedConstants) {
        return lineChangeManager.getUnusedWantedConstants(changes.values().stream().flatMap(List::stream).collect(Collectors.toList()), wantedConstants)
                                .collectList()
                                .filter(Predicate.not(List::isEmpty))
                                .doOnNext(unusedConstants -> consoleHelper.writeLine(String.format("Still some unused constants: %s", unusedConstants)))
                                .then();
    }

    private Mono<Void> checkIfUserAgree() {
        if (applyWithoutPrompt) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> consoleHelper.writeLine("Apply changes ? type 'y' to apply changes, or everything else to cancel"))
                   .then(Mono.fromSupplier(() -> new Scanner(System.in).next()))
                   .filter("y"::equals)
                   .switchIfEmpty(Mono.error(() -> new ManuallyStoppedException("User refused to apply")))
                   .then();
    }

    public Mono<Void> applyAndSaveChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .filter(e -> onlyChangedFile(e.getValue()))
                   .groupBy(Map.Entry::getKey, Map.Entry::getValue)
                   .flatMap(fileChanges -> fileHelper.write(fileChanges.key(), true, fileChanges.flatMap(this::applyChanges)))
                   .then();
    }

    private boolean onlyChangedFile(final List<LineChange> changes) {
        return changes.stream().anyMatch(this::isModifyingChange);
    }

    private boolean isModifyingChange(final LineChange change) {
        return !LineChange.DiffEnum.DO_NOTHING.equals(change.getDiff());
    }

    public Flux<String> applyChanges(final Collection<LineChange> changes) {
        return Flux.fromIterable(changes).flatMap(lineChangeManager::applyChange);
    }
}
