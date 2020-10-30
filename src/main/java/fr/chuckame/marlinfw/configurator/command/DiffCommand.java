package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeFormatter;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Parameters(commandNames = "diff", commandDescription = "Display differences between marlin configuration files")
@RequiredArgsConstructor
public class DiffCommand implements Command {
    @Parameter(names = {"--left"}, variableArity = true, required = true, description = "marlin configuration files paths for the base of diff")
    private List<Path> leftFiles;
    @Parameter(names = {"--right"}, variableArity = true, required = true, description = "marlin configuration files paths to know what was changed since --source paths")
    private List<Path> rightFiles;

    private final LineChangeManager lineChangeManager;
    private final LineChangeFormatter lineChangeFormatter;
    private final FileHelper fileHelper;
    private final ConsoleHelper consoleHelper;
    private final ConstantLineInterpreter constantLineInterpreter;

    @Override
    public Mono<Void> run() {
        return getConstants(leftFiles)
                .zipWith(getConstants(rightFiles))
                .map(t -> Maps.difference(t.getT1(), t.getT2()))
                .flatMap(this::printDiff)
                .then();
    }

    private Mono<Void> printDiff(final MapDifference<String, Constant> diff) {
        final var removed = Flux.fromIterable(diff.entriesOnlyOnLeft().keySet())
                                .map(removedConstant -> removedConstant + ": Removed")
                                .doOnNext(msg -> consoleHelper.writeLine(msg, ConsoleHelper.ColorEnum.RED))
                                .then();
        final var added = Flux.fromIterable(diff.entriesOnlyOnRight().values())
                              .map(addedConstant -> {
                                  if (addedConstant.isEnabled() && addedConstant.getValue() != null) {
                                      return addedConstant.getName() + ": Added with value: '" + addedConstant.getValue() + "'";
                                  }
                                  return addedConstant.getName() + ": Added";
                              })
                              .doOnNext(msg -> consoleHelper.writeLine(msg, ConsoleHelper.ColorEnum.GREEN))
                              .then();
        final var modified = Flux.fromIterable(diff.entriesDiffering().values())
                                 .map(modifiedConstant -> lineChangeManager.toLineChange("", 1, modifiedConstant.leftValue(), modifiedConstant.rightValue()))
                                 .map(lineChangeFormatter::format)
                                 .doOnNext(msg -> consoleHelper.writeLine(msg, ConsoleHelper.ColorEnum.CYAN))
                                 .then();
        return Flux.concat(added, removed, Mono.fromRunnable(() -> consoleHelper.writeLine("Modified:", ConsoleHelper.ColorEnum.CYAN)), modified).then();
    }

    private Mono<Map<String, Constant>> getConstants(final List<Path> files) {
        return Flux.fromIterable(files)
                   .flatMap(fileHelper::lines)
                   .flatMap(constantLineInterpreter::parseLine)
                   .map(ConstantLineInterpreter.ParsedConstant::getConstant)
                   .collectMap(Constant::getName);
    }

    private Mono<Void> printChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .concatMap(fileChanges -> Flux.concat(
                           Flux.just(String.format("%s change(s) for file %s:", fileChanges.getValue().size(), fileChanges.getKey())),
                           Flux.fromIterable(fileChanges.getValue()).filter(LineChange::isConstant).map(lineChangeFormatter::format),
                           Flux.just("")
                   ))
                   .doOnNext(consoleHelper::writeLine)
                   .then()
                ;
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

    private Mono<Void> printUnusedConstants(final Map<Path, List<LineChange>> changes, final Map<String, Constant> wantedConstants) {
        return lineChangeManager.getUnusedWantedConstants(changes.values().stream().flatMap(List::stream).collect(Collectors.toList()), wantedConstants)
                                .collectList()
                                .filter(Predicate.not(List::isEmpty))
                                .doOnNext(unusedConstants -> consoleHelper.writeLine(String.format("Still some unused constants: %s%n", unusedConstants)))
                                .then();
    }
}
