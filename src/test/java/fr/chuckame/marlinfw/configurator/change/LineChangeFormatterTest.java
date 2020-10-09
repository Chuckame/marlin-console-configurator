package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineChangeFormatterTest {
    public static final String CONSTANT_NAME = "constant";
    public static final String CONSTANT_VALUE = "value";
    public static final String LINE = "a line";
    public static final int LINE_NUMBER = 15;
    private final LineChangeFormatter formatter = new LineChangeFormatter();

    @Test
    void formatShouldReturnNull() {
        final var lineChange = new LineChange(LINE, LINE_NUMBER);

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isNull();
    }

    @Test
    void formatShouldReturnDoNothingWhenWantedConstantIsNull() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).build())
                .wantedConstant(null)
                .enabledDiff(LineChange.EnabledDiffEnum.DO_NOTHING)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": Nothing to do");
    }

    @Test
    void formatShouldReturnDoNothingWhenEnabledAndValuesAreSame() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).build())
                .wantedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).build())
                .enabledDiff(LineChange.EnabledDiffEnum.DO_NOTHING)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": Nothing to do");
    }

    @Test
    void formatShouldReturnDoNothingWhenEnabledAndValuesAreNull() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).build())
                .wantedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).build())
                .enabledDiff(LineChange.EnabledDiffEnum.DO_NOTHING)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": Nothing to do");
    }

    @Test
    void formatShouldReturnEnableOnlyWhenParsedDisabledAndWantedEnabled() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE).build())
                .wantedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).build())
                .enabledDiff(LineChange.EnabledDiffEnum.TO_ENABLE)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": E");
    }

    @Test
    void formatShouldReturnDisableOnlyWhenParsedEnabledAndWantedDisabled() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).build())
                .wantedConstant(Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE).build())
                .enabledDiff(LineChange.EnabledDiffEnum.TO_DISABLE)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": D");
    }

    @Test
    void formatShouldReturnEnableAndChangeWhenParsedDisabledAndWantedEnabledWithOtherValue() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(false).value(CONSTANT_VALUE).build())
                .wantedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value("new value").build())
                .enabledDiff(LineChange.EnabledDiffEnum.TO_ENABLE)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": E&C value → new value");
    }

    @Test
    void formatShouldReturnChangeOnlyWhenEnabledAndWantedWithOtherValue() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value(CONSTANT_VALUE).build())
                .wantedConstant(Constant.builder().name(CONSTANT_NAME).enabled(true).value("new value").build())
                .enabledDiff(LineChange.EnabledDiffEnum.DO_NOTHING)
                .build();

        final var formatted = formatter.format(lineChange);

        assertThat(formatted).isEqualTo(CONSTANT_NAME + ": C value → new value");
    }

    private LineChange.LineChangeBuilder lineChange() {
        return LineChange.builder().line(LINE).lineNumber(LINE_NUMBER);
    }
}
