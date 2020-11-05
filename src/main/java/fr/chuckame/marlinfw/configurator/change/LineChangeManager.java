package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineDetails;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LineChangeManager {
    private final ConstantLineInterpreter constantLineInterpreter;
    private final LineChangeValidator lineChangeValidator;

    public Mono<LineChange> prepareChange(final String line, final int lineNumber, final Map<String, Constant> wantedConstants) {
        return constantLineInterpreter.parseLine(line)
                                      .map(parsedConstant -> toLineChange(line, lineNumber, parsedConstant, wantedConstants
                                              .get(parsedConstant.getConstant().getName())))
                                      .switchIfEmpty(Mono.fromSupplier(() -> new LineChange(line, lineNumber)));
    }

    public Mono<String> applyChange(final LineChange change) {
        if (!change.isConstant()) {
            return Mono.fromSupplier(change::getLine);
        }
        switch (change.getDiff()) {
            case ERROR:
                return Mono.error(() -> new BadLineChangeException(change.getViolation(), change));
            case CHANGE_VALUE:
                return constantLineInterpreter.changeValue(change.getConstantLineDetails(), change.getConstant().getWantedValue());
            case TO_ENABLE_AND_CHANGE_VALUE:
                return constantLineInterpreter.enableLineAndChangeValue(change.getConstantLineDetails(), change.getConstant().getWantedValue());
            case TO_ENABLE:
                return constantLineInterpreter.enableLine(change.getConstantLineDetails());
            case TO_DISABLE:
                return constantLineInterpreter.disableLine(change.getConstantLineDetails());
            case DO_NOTHING:
                return Mono.just(change.getLine());
            default:
                throw new UnsupportedOperationException("Unknown diff: " + change.getDiff());
        }
    }

    public Flux<String> getUnusedWantedConstants(final Collection<LineChange> changes, final Map<String, Constant> wantedConstants) {
        final List<String> constantsFound = changes.stream()
                                                   .filter(LineChange::isConstant)
                                                   .map(LineChange::getConstant)
                                                   .map(LineChange.LineChangeConstant::getName)
                                                   .collect(Collectors.toList());
        return Flux.fromIterable(wantedConstants.keySet())
                   .filter(Predicate.not(constantsFound::contains));
    }

    public LineChange toLineChange(final String line, final int lineNumber, final Constant parsedConstant,
                                   @Nullable final Constant wantedConstant, @Nullable final ConstantLineDetails lineDetails) {
        final var violation = lineChangeValidator.getViolation(parsedConstant, wantedConstant);
        return LineChange.builder()
                         .line(line)
                         .lineNumber(lineNumber)
                         .constant(LineChange.LineChangeConstant.builder()
                                                                .name(parsedConstant.getName())
                                                                .comment(parsedConstant.getComment())
                                                                .currentValue(parsedConstant.getValue())
                                                                .wantedValue(Optional.ofNullable(wantedConstant).map(Constant::getValue).orElse(null))
                                                                .build())
                         .diff(violation != null ? LineChange.DiffEnum.ERROR : computeDiff(parsedConstant, wantedConstant))
                         .violation(violation)
                         .constantLineDetails(lineDetails)
                         .build();
    }

    private LineChange toLineChange(final String line, final int lineNumber, final ConstantLineInterpreter.ParsedConstant parsedConstant, @Nullable final Constant wantedConstant) {
        return toLineChange(line, lineNumber, parsedConstant.getConstant(), wantedConstant, parsedConstant.getConstantLineDetails());
    }

    private LineChange.DiffEnum computeDiff(final Constant parsedConstant, @Nullable final Constant wantedConstant) {
        if (parsedConstant == null || wantedConstant == null) {
            return LineChange.DiffEnum.DO_NOTHING;
        }
        if (!parsedConstant.isEnabled() && wantedConstant.isEnabled() && !valueEquals(parsedConstant, wantedConstant)) {
            return LineChange.DiffEnum.TO_ENABLE_AND_CHANGE_VALUE;
        }
        if (!parsedConstant.isEnabled() && wantedConstant.isEnabled() && valueEquals(parsedConstant, wantedConstant)) {
            return LineChange.DiffEnum.TO_ENABLE;
        }
        if (parsedConstant.isEnabled() && wantedConstant.isEnabled() && !valueEquals(parsedConstant, wantedConstant)) {
            return LineChange.DiffEnum.CHANGE_VALUE;
        }
        if (parsedConstant.isEnabled() && !wantedConstant.isEnabled()) {
            return LineChange.DiffEnum.TO_DISABLE;
        }
        return LineChange.DiffEnum.DO_NOTHING;
    }

    private boolean valueEquals(final Constant parsedConstant, final Constant wantedConstant) {
        return Objects.equals(parsedConstant.getValue(), wantedConstant.getValue());
    }
}
