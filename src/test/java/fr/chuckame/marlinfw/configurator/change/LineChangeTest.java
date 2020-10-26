package fr.chuckame.marlinfw.configurator.change;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineChangeTest {
    private static final String CONSTANT_VALUE = "value";

    @Test
    void isConstantShouldReturnFalseWhenParsedConstantIsNull() {
        final var lineChange = lineChange()
                .constant(null)
                .build();

        final var isConstant = lineChange.isConstant();

        assertThat(isConstant).isFalse();
    }

    @Test
    void isConstantShouldReturnTrueWhenParsedConstantIsNotNull() {
        final var lineChange = lineChange()
                .build();

        final var isConstant = lineChange.isConstant();

        assertThat(isConstant).isTrue();
    }

    private LineChange.LineChangeBuilder lineChange() {
        return LineChange.builder()
                         .line("a line")
                         .lineNumber(15)
                         .diff(LineChange.DiffEnum.DO_NOTHING)
                         .constant(LineChange.LineChangeConstant.builder()
                                                                .currentValue(CONSTANT_VALUE)
                                                                .wantedValue(CONSTANT_VALUE)
                                                                .build());
    }
}
