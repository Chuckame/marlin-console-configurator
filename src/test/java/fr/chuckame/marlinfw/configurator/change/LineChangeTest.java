package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineChangeTest {

    public static final String CONSTANT_VALUE = "value";

    @Test
    void isValueChangedShouldReturnFalseWhenParsedConstantIsNull() {
        final var lineChange = lineChange()
                .parsedConstant(null)
                .build();

        final var isConstant = lineChange.isValueChanged();

        assertThat(isConstant).isFalse();
    }

    @Test
    void isValueChangedShouldReturnFalseWhenWantedConstantIsNull() {
        final var lineChange = lineChange()
                .wantedConstant(null)
                .build();

        final var isConstant = lineChange.isValueChanged();

        assertThat(isConstant).isFalse();
    }

    @Test
    void isValueChangedShouldReturnTrueWhenParsedValueIsDifferentThanWantedValue() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().value("other").build())
                .build();

        final var isConstant = lineChange.isValueChanged();

        assertThat(isConstant).isTrue();
    }

    @Test
    void isValueChangedShouldReturnTrueWhenWantedValueIsDifferentThanParsedValue() {
        final var lineChange = lineChange()
                .wantedConstant(Constant.builder().value("other").build())
                .build();

        final var isConstant = lineChange.isValueChanged();

        assertThat(isConstant).isTrue();
    }

    @Test
    void isValueChangedShouldReturnFalseWhenWantedValueIsSameAsParsedValue() {
        final var lineChange = lineChange().build();

        final var isConstant = lineChange.isValueChanged();

        assertThat(isConstant).isFalse();
    }

    @Test
    void isConstantShouldReturnFalseWhenParsedConstantIsNull() {
        final var lineChange = lineChange()
                .parsedConstant(null)
                .build();

        final var isConstant = lineChange.isConstant();

        assertThat(isConstant).isFalse();
    }

    @Test
    void isConstantShouldReturnTrueWhenParsedConstantIsNotNull() {
        final var lineChange = lineChange()
                .parsedConstant(Constant.builder().build())
                .build();

        final var isConstant = lineChange.isConstant();

        assertThat(isConstant).isTrue();
    }

    @Test
    void isConstantShouldReturnFalseWhenWantedConstantIsNull() {
        final var lineChange = lineChange()
                .wantedConstant(null)
                .build();

        final var hasWantedConstant = lineChange.hasWantedConstant();

        assertThat(hasWantedConstant).isFalse();
    }

    @Test
    void isConstantShouldReturnTrueWhenWantedConstantIsNotNull() {
        final var lineChange = lineChange()
                .wantedConstant(Constant.builder().build())
                .build();

        final var hasWantedConstant = lineChange.hasWantedConstant();

        assertThat(hasWantedConstant).isTrue();
    }

    private LineChange.LineChangeBuilder lineChange() {
        return LineChange.builder()
                         .line("a line")
                         .lineNumber(15)
                         .enabledDiff(LineChange.EnabledDiffEnum.DO_NOTHING)
                         .wantedConstant(Constant.builder().value(CONSTANT_VALUE).build())
                         .parsedConstant(Constant.builder().value(CONSTANT_VALUE).build());
    }
}
