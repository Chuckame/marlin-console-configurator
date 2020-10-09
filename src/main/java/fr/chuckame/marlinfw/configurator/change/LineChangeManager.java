package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineParser;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class LineChangeManager {
    private final ConstantLineParser constantLineParser;
    private final LineChangeValidator lineChangeValidator;

    public Mono<LineChange> prepareChange(final String line, final int lineNumber, final Map<String, Constant> wantedConstants) {
        return constantLineParser.parseLine(line)
                                 .map(parsedConstant -> toLineChange(line, lineNumber, parsedConstant, wantedConstants.get(parsedConstant.getName())))
                                 .switchIfEmpty(Mono.fromSupplier(() -> new LineChange(line, lineNumber)));
    }

    public String applyChange(final LineChange change) {
        final var violation = lineChangeValidator.getViolation(change);
        if (violation != null) {
            throw new BadLineChangeException(violation, change);
        }
        if (change.getParsedConstant() == null || change.getWantedConstant() == null) {
            return change.getLine();
        }
        throw new UnsupportedOperationException();
    }

    private LineChange toLineChange(final String line, final int lineNumber, final Constant parsedConstant, @Nullable final Constant wantedConstant) {
        return LineChange.builder()
                         .line(line)
                         .lineNumber(lineNumber)
                         .parsedConstant(parsedConstant)
                         .wantedConstant(wantedConstant)
                         .enabledDiff(computeEnabledDiff(parsedConstant.isEnabled(), Optional.ofNullable(wantedConstant).map(Constant::isEnabled).orElse(null)))
                         .build();
    }

    private LineChange.EnabledDiffEnum computeEnabledDiff(final boolean parsedConstantIsEnabled, @Nullable final Boolean wantedConstantIsEnabled) {
        if (wantedConstantIsEnabled == null || parsedConstantIsEnabled == wantedConstantIsEnabled) {
            return LineChange.EnabledDiffEnum.DO_NOTHING;
        }
        return wantedConstantIsEnabled ? LineChange.EnabledDiffEnum.TO_ENABLE : LineChange.EnabledDiffEnum.TO_DISABLE;
    }
}
