package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E/v -> E/NO_VALUE : ERROR<br>
 * E/NO_VALUE -> E/v : ERROR<br>
 * D/v -> E/NO_VALUE : ERROR<br>
 * D/NO_VALUE -> E/v : ERROR<br>
 * All other cases are good: no violation
 */
class LineChangeValidatorTest {
    public static final String CONSTANT_NAME = "constant";
    public static final String CONSTANT_VALUE = "value1";

    private final LineChangeValidator lineChangeValidator = new LineChangeValidator();

    @Test
    void getViolationShouldReturnViolationWhenEnabledButCurrentWithValueAndWantedWithoutValue() {
        // E/v -> E/NO_VALUE : ERROR
        final var lineChange = lineChange(
                parsed -> parsed.enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE),
                wanted -> wanted.enabled(true).name(CONSTANT_NAME)
        );

        final var violation = lineChangeValidator.getViolation(lineChange);

        assertThat(violation).contains("Wanted value should be defined");
    }

    @Test
    void getViolationShouldReturnViolationWhenCurrentDisabledWithValueAndWantedEnabledWithoutValue() {
        // D/v -> E/NO_VALUE : ERROR
        final var lineChange = lineChange(
                parsed -> parsed.enabled(false).name(CONSTANT_NAME).value(CONSTANT_VALUE),
                wanted -> wanted.enabled(true).name(CONSTANT_NAME)
        );

        final var violation = lineChangeValidator.getViolation(lineChange);

        assertThat(violation).contains("Wanted value should be defined");
    }

    @Test
    void getViolationShouldReturnViolationWhenEnabledCurrentWithoutValueAndWantedWithValue() {
        // E/NO_VALUE -> E/v : ERROR
        final var lineChange = lineChange(
                parsed -> parsed.enabled(true).name(CONSTANT_NAME),
                wanted -> wanted.enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE)
        );

        final var violation = lineChangeValidator.getViolation(lineChange);

        assertThat(violation).contains("Wanted value should not be defined");
    }

    @Test
    void getViolationShouldReturnViolationWhenDisabledCurrentWithoutValueAndEnabledWantedWithValue() {
        // D/NO_VALUE -> E/v : ERROR
        final var lineChange = lineChange(
                parsed -> parsed.enabled(false).name(CONSTANT_NAME),
                wanted -> wanted.enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE)
        );

        final var violation = lineChangeValidator.getViolation(lineChange);

        assertThat(violation).contains("Wanted value should not be defined");
    }

    @ParameterizedTest
    @MethodSource("getViolationShouldReturnNullArguments")
    void getViolationShouldReturnNull(final Constant.ConstantBuilder currentConstant, final Constant.ConstantBuilder wantedConstant) {
        final var lineChange = lineChange(b -> currentConstant, b -> wantedConstant);

        final var violation = lineChangeValidator.getViolation(lineChange);

        assertThat(violation).isNull();
    }

    private static Stream<Arguments> getViolationShouldReturnNullArguments() {
        return Stream.of(
                // SameEnabledConstantsIgnoringComment
                Arguments.arguments(Constant.builder().enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE).comment("comment1"),
                                    Constant.builder().enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE).comment("comment2")),
                // SameDisabledConstantsIgnoringComment
                Arguments.arguments(Constant.builder().enabled(false).name(CONSTANT_NAME).value(CONSTANT_VALUE).comment("comment1"),
                                    Constant.builder().enabled(false).name(CONSTANT_NAME).value(CONSTANT_VALUE).comment("comment2")),
                // E/v -> D/v
                Arguments.arguments(Constant.builder().enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE),
                                    Constant.builder().enabled(false).name(CONSTANT_NAME).value(CONSTANT_VALUE)),
                // D/v -> E/v
                Arguments.arguments(Constant.builder().enabled(false).name(CONSTANT_NAME).value(CONSTANT_VALUE),
                                    Constant.builder().enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE)),
                // E/NO_VALUE -> D/NO_VALUE
                Arguments.arguments(Constant.builder().enabled(true).name(CONSTANT_NAME),
                                    Constant.builder().enabled(false).name(CONSTANT_NAME)),
                // D/NO_VALUE -> E/NO_VALUE
                Arguments.arguments(Constant.builder().enabled(false).name(CONSTANT_NAME),
                                    Constant.builder().enabled(true).name(CONSTANT_NAME))
        );
    }

    private LineChange lineChange(final Function<Constant.ConstantBuilder, Constant.ConstantBuilder> parsedConstant,
                                  final Function<Constant.ConstantBuilder, Constant.ConstantBuilder> wantedConstant) {
        final var parsed = parsedConstant.apply(Constant.builder()).build();
        final var wanted = wantedConstant.apply(Constant.builder()).build();
        final var enabledDiff = parsed.isEnabled() == wanted.isEnabled() ? LineChange.EnabledDiffEnum.DO_NOTHING :
                (wanted.isEnabled() ? LineChange.EnabledDiffEnum.TO_ENABLE : LineChange.EnabledDiffEnum.TO_DISABLE);
        return LineChange.builder()
                         .parsedConstant(parsed)
                         .wantedConstant(wanted)
                         .lineNumber(15)
                         .line("a line")
                         .enabledDiff(enabledDiff)
                         .build();
    }
}
