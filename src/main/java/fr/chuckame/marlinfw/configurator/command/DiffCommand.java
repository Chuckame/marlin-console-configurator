package fr.chuckame.marlinfw.configurator.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import fr.chuckame.marlinfw.configurator.change.LineChangeFormatter;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.profile.ConstantHelper;
import fr.chuckame.marlinfw.configurator.util.ConsoleHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

@Component
@Parameters(commandNames = "diff", commandDescription = "Display differences between marlin configuration files")
@RequiredArgsConstructor
public class DiffCommand implements Command {
    @Parameter(names = {"--left"}, variableArity = true, required = true, description = "marlin configuration folder or files paths for the base of diff")
    private List<Path> leftFiles;
    @Parameter(names = {"--right"}, variableArity = true, required = true, description = "marlin configuration folder or files paths to know what was changed since --source paths")
    private List<Path> rightFiles;

    private final LineChangeManager lineChangeManager;
    private final LineChangeFormatter lineChangeFormatter;
    private final ConstantHelper constantHelper;
    private final ConsoleHelper consoleHelper;

    @Override
    public Mono<Void> run() {
        return Mono.zip(constantHelper.getConstants(leftFiles).collectMap(Constant::getName), constantHelper.getConstants(rightFiles).collectMap(Constant::getName))
                   .map(t -> Maps.difference(t.getT1(), t.getT2()))
                   .flatMap(this::printDiff)
                   .then();
    }

    private Mono<Void> printDiff(final MapDifference<String, Constant> diff) {
        final var added = Flux.fromIterable(diff.entriesOnlyOnRight().values())
                              .map(addedConstant -> {
                                  if (addedConstant.isEnabled() && addedConstant.getValue() != null) {
                                      return addedConstant.getName() + ": " + addedConstant.getValue();
                                  }
                                  return addedConstant.getName();
                              })
                              .doOnNext(msg -> consoleHelper.writeLine(msg, ConsoleHelper.ForegroundColorEnum.GREEN))
                              .then();
        final var removed = Flux.fromIterable(diff.entriesOnlyOnLeft().keySet())
                                .doOnNext(removedConstant -> consoleHelper.writeLine(removedConstant, ConsoleHelper.ForegroundColorEnum.RED))
                                .then();
        final var modified = Flux.fromIterable(diff.entriesDiffering().values())
                                 .map(modifiedConstant -> lineChangeManager.toLineChange("", 1, modifiedConstant.leftValue(), modifiedConstant.rightValue(), null))
                                 .map(lineChangeFormatter::format)
                                 .doOnNext(msg -> consoleHelper.writeLine(msg, ConsoleHelper.ForegroundColorEnum.CYAN))
                                 .then();
        return Flux.concat(
                Mono.fromRunnable(() -> consoleHelper
                        .writeLine("Present in right (or absent in left):", ConsoleHelper.ForegroundColorEnum.GREEN, ConsoleHelper.FormatterEnum.UNDERLINED)),
                added,
                Mono.fromRunnable(() -> consoleHelper.writeLine("Modified:", ConsoleHelper.ForegroundColorEnum.CYAN, ConsoleHelper.FormatterEnum.UNDERLINED)),
                modified,
                Mono.fromRunnable(() -> consoleHelper
                        .writeLine("Absent in right (or present in left):", ConsoleHelper.ForegroundColorEnum.RED, ConsoleHelper.FormatterEnum.UNDERLINED)),
                removed
        ).then();
    }
}
