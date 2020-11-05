package fr.chuckame.marlinfw.configurator.change;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineChangeFormatterTest {
    private static final String CONSTANT_NAME = "constant";
    private static final String CONSTANT_VALUE = "value";
    private static final String LINE = "a line";
    private static final int LINE_NUMBER = 15;
    private final LineChangeFormatter formatter = new LineChangeFormatter();

    @Test
    void formatShouldReturnNull() {
        final var lineChange = new LineChange(LINE, LINE_NUMBER);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isNull();
    }

    @Test
    void formatShouldReturnDoNothingWhenWantedConstantIsNull() {
        final var lineChange = lineChange(LineChange.DiffEnum.DO_NOTHING, CONSTANT_VALUE, null);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": Nothing to do");
    }

    @Test
    void formatShouldReturnDoNothingWhenEnabledAndValuesAreSame() {
        final var lineChange = lineChange(LineChange.DiffEnum.DO_NOTHING, CONSTANT_VALUE, CONSTANT_VALUE);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": Nothing to do");
    }

    @Test
    void formatShouldReturnDoNothingWhenEnabledAndValuesAreNull() {
        final var lineChange = lineChange(LineChange.DiffEnum.DO_NOTHING, null, null);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": Nothing to do");
    }

    @Test
    void formatShouldReturnEnableOnlyWhenParsedDisabledAndWantedEnabled() {
        final var lineChange = lineChange(LineChange.DiffEnum.TO_ENABLE, CONSTANT_VALUE, CONSTANT_VALUE);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo("Enable          constant");
    }

    @Test
    void formatShouldReturnDisableOnlyWhenParsedEnabledAndWantedDisabled() {
        final var lineChange = lineChange(LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, CONSTANT_VALUE);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo("Disable         constant");
    }

    @Test
    void formatShouldReturnEnableAndChangeWhenParsedDisabledAndWantedEnabledWithOtherValue() {
        final var lineChange = lineChange(LineChange.DiffEnum.TO_ENABLE_AND_CHANGE_VALUE, CONSTANT_VALUE, "new value");

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo("Enable & Change constant: value → new value");
    }

    @Test
    void formatShouldReturnExpectedTextWhenChangeValue() {
        final var lineChange = lineChange(LineChange.DiffEnum.CHANGE_VALUE, CONSTANT_VALUE, "new value");

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo("Change          constant: value → new value");
    }

    @Test
    void formatShouldReturnExpectedTextWhenError() {
        final var lineChange = LineChange.builder()
                                         .line(LINE)
                                         .lineNumber(LINE_NUMBER)
                                         .diff(LineChange.DiffEnum.ERROR)
                                         .violation("a violation")
                                         .constant(LineChange.LineChangeConstant.builder()
                                                                                .name(CONSTANT_NAME)
                                                                                .currentValue(CONSTANT_VALUE)
                                                                                .build())
                                         .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo("Error           constant: a violation");
    }

    private LineChange lineChange(final LineChange.DiffEnum diff, final String oldValue, final String wantedValue) {
        return LineChange.builder()
                         .line(LINE)
                         .lineNumber(LINE_NUMBER)
                         .diff(diff)
                         .constant(LineChange.LineChangeConstant.builder()
                                                                .name(CONSTANT_NAME)
                                                                .currentValue(oldValue)
                                                                .wantedValue(wantedValue)
                                                                .build())
                         .build();
    }
}
