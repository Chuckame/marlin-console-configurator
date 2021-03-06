package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeFormatter;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ProfileAdapter;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Parameters(commandNames = "apply", commandDescription = "Apply the given profile to marlin constants files, that will enable, change value or disable constants into marlin configuration files")
@RequiredArgsConstructor
public class ApplyCommand implements Command {
    @Parameter(required = true, description = "/path1 /path2 ...\tFile or directory path(s) where all changes will be applied")
    private List<Path> filesPath;
    @Parameter(names = {"--profiles", "-p"}, variableArity = true, required = true, description = "Profile's path(s) (space separated) containing changes to apply. Format: yaml")
    private List<Path> profilePaths;
    @Parameter(names = {"--save", "-s"}, description = "When is present, will save changes to files. Else, just display changes without saving")
    private boolean doSave;
    @Parameter(names = {"--yes", "-y"}, description = "when present, the changes will be saved without prompting the user")
    private boolean applyWithoutPrompt;
    @Parameter(names = {"--verbose", "-v"}, description = "when present, all non-changed line are printed")
    private boolean verbose;

    private final ProfileAdapter profileAdapter;
    private final LineChangeManager lineChangeManager;
    private final LineChangeFormatter lineChangeFormatter;
    private final ProfilePropertiesParser profilePropertiesParser;
    private final FileHelper fileHelper;
    private final ConsoleHelper consoleHelper;

    @Override
    public Mono<Void> run() {
        return profilePropertiesParser
                .parseFromFiles(profilePaths)
                .map(profileAdapter::profileToConstants)
                .flatMap(wantedConstants ->
                                 prepareChanges(wantedConstants)
                                         .flatMap(changes -> printChanges(changes)
                                                 .then(printUnusedConstants(changes, wantedConstants))
                                                 .then(applyAndSaveChangesIfNeeded(changes)))
                );
    }

    private Mono<Map<Path, List<LineChange>>> prepareChanges(final Map<String, Constant> wantedConstants) {
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
                           Flux.fromIterable(fileChanges.getValue())
                               .filter(this::isModifyingChange)
                               .count()
                               .filter(count -> verbose || count > 0)
                               .doOnNext(count -> consoleHelper.writeLine(String.format("%s change(s) to apply for file %s:", count, fileChanges
                                       .getKey()), ConsoleHelper.FormatterEnum.UNDERLINED, ConsoleHelper.FormatterEnum.BOLD, ConsoleHelper.ForegroundColorEnum.GREEN)),
                           Flux.fromIterable(fileChanges.getValue())
                               .filter(LineChange::isConstant)
                               .filter(this::isModifyingChange)
                               .doOnNext(change -> consoleHelper.writeLine(lineChangeFormatter.format(change), getChangeColor(change))),
                           Flux.fromIterable(fileChanges.getValue())
                               .filter(LineChange::isConstant)
                               .filter(c -> verbose && !isModifyingChange(c))
                               .doOnNext(change -> consoleHelper.writeLine(lineChangeFormatter.format(change), getChangeColor(change))),
                           Mono.fromRunnable(consoleHelper::newLine)
                   ))
                   .then()
                ;
    }

    private ConsoleHelper.ConsoleStyle getChangeColor(final LineChange change) {
        switch (change.getDiff()) {
            case ERROR:
                return ConsoleHelper.ForegroundColorEnum.RED;
            case CHANGE_VALUE:
                return ConsoleHelper.ForegroundColorEnum.LIGHT_BLUE;
            case TO_DISABLE:
                return ConsoleHelper.ForegroundColorEnum.LIGHT_YELLOW;
            case TO_ENABLE:
                return ConsoleHelper.ForegroundColorEnum.LIGHT_CYAN;
            case TO_ENABLE_AND_CHANGE_VALUE:
                return ConsoleHelper.ForegroundColorEnum.LIGHT_MAGENTA;
            case DO_NOTHING:
            default:
                return ConsoleHelper.ForegroundColorEnum.DARK_GRAY;
        }
    }

    private Mono<Void> printUnusedConstants(final Map<Path, List<LineChange>> changes, final Map<String, Constant> wantedConstants) {
        return lineChangeManager.getUnusedWantedConstants(changes.values().stream().flatMap(List::stream).collect(Collectors.toList()), wantedConstants)
                                .collectList()
                                .filter(Predicate.not(List::isEmpty))
                                .doOnNext(unusedConstants -> consoleHelper.writeLine(String.format("Still some unused constants: %s", unusedConstants)))
                                .then();
    }

    private Mono<Void> applyAndSaveChangesIfNeeded(final Map<Path, List<LineChange>> changes) {
        if (!doSave) {
            return Mono.empty();
        }
        return checkIfUserAgree().then(applyAndSaveChanges(changes));
    }

    private Mono<Void> checkIfUserAgree() {
        if (applyWithoutPrompt) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> consoleHelper.writeLine("Apply changes ? type 'y' to apply changes, or everything else to cancel"))
                   .then(Mono.fromSupplier(consoleHelper::readLine))
                   .filter("y"::equals)
                   .switchIfEmpty(Mono.error(() -> new ManuallyStoppedException("User refused to apply")))
                   .then();
    }

    private Mono<Void> applyAndSaveChanges(final Map<Path, List<LineChange>> changes) {
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

    private Flux<String> applyChanges(final Collection<LineChange> changes) {
        return Flux.fromIterable(changes).flatMap(lineChangeManager::applyChange);
    }
}
