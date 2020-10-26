package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
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
    private static final String CONSTANT_NAME = "constant";
    private static final String CONSTANT_VALUE = "value1";

    private final LineChangeValidator lineChangeValidator = new LineChangeValidator();

    @Test
    void getViolationShouldReturnNullWhenGivenLineHasNoWantedConstant() {
        final Constant parsed = Constant.builder()
                                        .value(CONSTANT_VALUE)
                                        .build();
        final Constant wanted = null;

        final var violation = lineChangeValidator.getViolation(parsed, wanted);

        assertThat(violation).isNull();
    }

    @Test
    void getViolationShouldReturnNullWhenGivenLineIsNotAConstant() {
        final Constant parsed = null;
        final Constant wanted = null;

        final var violation = lineChangeValidator.getViolation(parsed, wanted);

        assertThat(violation).isNull();
    }

    @Test
    void getViolationShouldReturnViolationWhenEnabledButCurrentWithValueAndWantedWithoutValue() {
        // E/v -> E/NO_VALUE : ERROR
        final Constant parsed = Constant.builder()
                                        .enabled(true)
                                        .value(CONSTANT_VALUE)
                                        .build();
        final Constant wanted = Constant.builder()
                                        .enabled(true)
                                        .build();

        final var violation = lineChangeValidator.getViolation(parsed, wanted);

        assertThat(violation).contains("Wanted value should be defined");
    }

    @Test
    void getViolationShouldReturnViolationWhenCurrentDisabledWithValueAndWantedEnabledWithoutValue() {
        // D/v -> E/NO_VALUE : ERROR
        final Constant parsed = Constant.builder()
                                        .enabled(false)
                                        .value(CONSTANT_VALUE)
                                        .build();
        final Constant wanted = Constant.builder()
                                        .enabled(true)
                                        .build();

        final var violation = lineChangeValidator.getViolation(parsed, wanted);

        assertThat(violation).contains("Wanted value should be defined");
    }

    @Test
    void getViolationShouldReturnViolationWhenEnabledButCurrentWithoutValueAndWantedWithValue() {
        // E/NO_VALUE -> E/v : ERROR
        final Constant parsed = Constant.builder()
                                        .enabled(true)
                                        .build();
        final Constant wanted = Constant.builder()
                                        .enabled(true)
                                        .value(CONSTANT_VALUE)
                                        .build();

        final var violation = lineChangeValidator.getViolation(parsed, wanted);

        assertThat(violation).contains("Wanted value should not be defined");
    }

    @Test
    void getViolationShouldReturnViolationWhenDisabledCurrentWithoutValueAndEnabledWantedWithValue() {
        // D/NO_VALUE -> E/v : ERROR
        final Constant parsed = Constant.builder()
                                        .enabled(false)
                                        .build();
        final Constant wanted = Constant.builder()
                                        .enabled(true)
                                        .value(CONSTANT_VALUE)
                                        .build();

        final var violation = lineChangeValidator.getViolation(parsed, wanted);

        assertThat(violation).contains("Wanted value should not be defined");
    }

    @ParameterizedTest
    @MethodSource("getViolationShouldReturnNullArguments")
    void getViolationShouldReturnNull(final Constant.ConstantBuilder currentConstant, final Constant.ConstantBuilder wantedConstant) {
        final var violation = lineChangeValidator.getViolation(Optional.ofNullable(currentConstant).map(Constant.ConstantBuilder::build).orElse(null),
                                                               Optional.ofNullable(wantedConstant).map(Constant.ConstantBuilder::build).orElse(null));

        assertThat(violation).isNull();
    }

    private static Stream<Arguments> getViolationShouldReturnNullArguments() {
        return Stream.of(
                // SameEnabledConstantsIgnoringComment
                Arguments.arguments(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).comment("comment1"),
                                    Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).comment("comment2")),
                // SameDisabledConstantsIgnoringComment
                Arguments.arguments(Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE).comment("comment1"),
                                    Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE).comment("comment2")),
                // E/v -> D/v
                Arguments.arguments(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE),
                                    Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE)),
                // D/v -> E/v
                Arguments.arguments(Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE),
                                    Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE)),
                // E/NO_VALUE -> D/NO_VALUE
                Arguments.arguments(Constant.builder().name(CONSTANT_NAME).enabled(true),
                                    Constant.builder().name(CONSTANT_NAME).enabled(false)),
                // D/NO_VALUE -> E/NO_VALUE
                Arguments.arguments(Constant.builder().name(CONSTANT_NAME).enabled(false),
                                    Constant.builder().name(CONSTANT_NAME).enabled(true))
        );
    }
}
