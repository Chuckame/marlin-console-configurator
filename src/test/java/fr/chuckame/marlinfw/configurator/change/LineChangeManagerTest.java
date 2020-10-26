package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineDetails;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineChangeManagerTest {
    private static final String CONSTANT_NAME = "name";
    private static final String CONSTANT_VALUE = "value";
    private static final String CONSTANT_OTHER_VALUE = "other value";
    private static final int LINE_NUMBER = 15;
    private static final String INPUT_LINE = "input";
    private static final ConstantLineDetails INPUT_LINE_DETAILS = ConstantLineDetails.builder()
                                                                                     .line(INPUT_LINE)
                                                                                     .build();
    private static final String OUTPUT_LINE = "output";

    @Mock
    private ConstantLineInterpreter constantLineInterpreterMock;
    @Mock
    private LineChangeValidator lineChangeValidatorMock;
    @InjectMocks
    private LineChangeManager lineChangeManager;

    @Test
    void getUnusedWantedConstantsShouldReturnEmptyWhenAllUsed() {
        final var changes = List.of(
                lineChange("c1", LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, CONSTANT_VALUE).build(),
                lineChange("c2", LineChange.DiffEnum.TO_ENABLE, CONSTANT_VALUE, CONSTANT_VALUE).build(),
                lineChange("c3", LineChange.DiffEnum.TO_ENABLE, CONSTANT_VALUE, CONSTANT_VALUE).build()
        );
        final var wantedConstants = changes.stream().collect(Collectors.toMap(c -> c.getConstant().getName(), this::lineChangeToWantedConstant));

        final var unusedWantedConstants = lineChangeManager.getUnusedWantedConstants(changes, wantedConstants);

        StepVerifier.create(unusedWantedConstants)
                    .expectComplete()
                    .verify();
    }

    private Constant lineChangeToWantedConstant(final LineChange change) {
        return Constant.builder()
                       .enabled(true)
                       .name(change.getConstant().getName())
                       .value(change.getConstant().getWantedValue())
                       .build();
    }

    @Test
    void getUnusedWantedConstantsShouldReturnEmptyWhenAllUsedAndMoreChangesThanWantedConstants() {
        final var changes = List.of(
                lineChange("c1", LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, CONSTANT_VALUE).build(),
                lineChange("c2", LineChange.DiffEnum.TO_ENABLE, CONSTANT_VALUE, CONSTANT_VALUE).build(),
                lineChange("c3", LineChange.DiffEnum.TO_ENABLE, CONSTANT_VALUE, CONSTANT_VALUE).build()
        );
        final var wantedConstants = Map.of("c1", lineChangeToWantedConstant(changes.get(0)));

        final var unusedWantedConstants = lineChangeManager.getUnusedWantedConstants(changes, wantedConstants);

        StepVerifier.create(unusedWantedConstants)
                    .expectComplete()
                    .verify();
    }

    @Test
    void getUnusedWantedConstantsShouldReturnOneElement() {
        final var changes = List.of(
                lineChange("c1", LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, CONSTANT_VALUE).build(),
                lineChange("c2", LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, CONSTANT_VALUE).build()
        );
        final var wantedConstants = Map.of("c2", lineChangeToWantedConstant(changes.get(1)),
                                           "c3", Constant.builder().name("c3").enabled(true).value("value").build());

        final var unusedWantedConstants = lineChangeManager.getUnusedWantedConstants(changes, wantedConstants);

        StepVerifier.create(unusedWantedConstants)
                    .expectNext("c3")
                    .expectComplete()
                    .verify();
    }

    @Test
    void getUnusedWantedConstantsShouldReturnTwoElements() {
        final var changes = List.of(
                lineChange("c1", LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, CONSTANT_VALUE).build()
        );
        final var wantedConstants = Map.of(
                "c2", Constant.builder().name("c2").enabled(false).build(),
                "c3", Constant.builder().name("c3").enabled(true).value("value").build()
        );

        final var unusedWantedConstants = lineChangeManager.getUnusedWantedConstants(changes, wantedConstants);

        assertThat(unusedWantedConstants.collectList().block()).containsExactlyInAnyOrder("c2", "c3");
    }

    @Test
    void prepareChangeShouldReturnNonConstantLineChangeWhenNothingParsed() {
        when(constantLineInterpreterMock.parseLine(INPUT_LINE)).thenReturn(Mono.empty());
        final Map<String, Constant> wantedConstants = Map.of();
        final var expectedChange = LineChange.builder()
                                             .diff(LineChange.DiffEnum.DO_NOTHING)
                                             .lineNumber(LINE_NUMBER)
                                             .line(INPUT_LINE)
                                             .build();

        final var change = lineChangeManager.prepareChange(INPUT_LINE, LINE_NUMBER, wantedConstants);

        StepVerifier.create(change)
                    .expectNext(expectedChange)
                    .expectComplete()
                    .verify();
    }

    @Test
    void prepareChangeShouldReturnDoNothingLineChangeWhenNoWantedConstant() {
        final var parsedConstant = parsedConstant(Constant.builder().name(CONSTANT_NAME).enabled(false).value("value").build());
        when(constantLineInterpreterMock.parseLine(INPUT_LINE)).thenReturn(Mono.just(parsedConstant));
        final Map<String, Constant> wantedConstants = Map.of();
        final var expectedChange = lineChange(LineChange.DiffEnum.DO_NOTHING, "value", null).build();

        final var change = lineChangeManager.prepareChange(INPUT_LINE, LINE_NUMBER, wantedConstants);

        StepVerifier.create(change)
                    .expectNext(expectedChange)
                    .expectComplete()
                    .verify();
    }

    @Test
    void prepareChangeShouldReturnDoNothingLineChange() {
        prepareChangeShouldReturnExpectedLineChange(true, "value", true, "value", LineChange.DiffEnum.DO_NOTHING);
    }

    @Test
    void prepareChangeShouldReturnDoNothingLineChange_nullValue() {
        prepareChangeShouldReturnExpectedLineChange(true, null, true, null, LineChange.DiffEnum.DO_NOTHING);
    }

    @Test
    void prepareChangeShouldReturnToEnableLineChange() {
        prepareChangeShouldReturnExpectedLineChange(false, "value", true, "value", LineChange.DiffEnum.TO_ENABLE);
    }

    @Test
    void prepareChangeShouldReturnToEnableAndChangeLineChange() {
        prepareChangeShouldReturnExpectedLineChange(false, "value", true, "other", LineChange.DiffEnum.TO_ENABLE_AND_CHANGE_VALUE);
    }

    @Test
    void prepareChangeShouldReturnToChangeLineChange() {
        prepareChangeShouldReturnExpectedLineChange(true, "value", true, "other", LineChange.DiffEnum.CHANGE_VALUE);
    }

    @Test
    void prepareChangeShouldReturnToDisableLineChange() {
        prepareChangeShouldReturnExpectedLineChange(true, "value", false, "other", LineChange.DiffEnum.TO_DISABLE);
    }

    void prepareChangeShouldReturnExpectedLineChange(final boolean parsedEnabled, final String parsedValue, final boolean wantedEnabled, final String wantedValue,
                                                     final LineChange.DiffEnum expectedDiff) {
        final var parsedConstant = Constant.builder().name(CONSTANT_NAME).enabled(parsedEnabled).value(parsedValue).build();
        final var wantedConstant = Constant.builder().name(CONSTANT_NAME).enabled(wantedEnabled).value(wantedValue).build();
        when(constantLineInterpreterMock.parseLine(INPUT_LINE)).thenReturn(Mono.just(parsedConstant(parsedConstant)));
        final Map<String, Constant> wantedConstants = Map.of(CONSTANT_NAME, wantedConstant);
        final var expectedChange = lineChange(expectedDiff, parsedValue, wantedValue).build();

        final var change = lineChangeManager.prepareChange(INPUT_LINE, LINE_NUMBER, wantedConstants);

        assertThat(change.blockOptional()).hasValue(expectedChange);
        verify(lineChangeValidatorMock).getViolation(parsedConstant, wantedConstant);
    }

    @Test
    void prepareChangeShouldReturnErrorLineChange() {
        final var parsedConstant = Constant.builder().name(CONSTANT_NAME).build();
        final var wantedConstant = Constant.builder().name(CONSTANT_NAME).build();
        when(constantLineInterpreterMock.parseLine(INPUT_LINE)).thenReturn(Mono.just(parsedConstant(parsedConstant)));
        when(lineChangeValidatorMock.getViolation(parsedConstant, wantedConstant)).thenReturn("an error");
        final Map<String, Constant> wantedConstants = Map.of(CONSTANT_NAME, wantedConstant);
        final var expectedChange = lineChange(LineChange.DiffEnum.ERROR, null, null)
                .violation("an error").build();

        final var change = lineChangeManager.prepareChange(INPUT_LINE, LINE_NUMBER, wantedConstants);

        assertThat(change.blockOptional()).hasValue(expectedChange);
    }

    @Test
    void applyChangeShouldReturnSameLineChangeWhenNotConstant() {
        final var lineChange = LineChange.builder()
                                         .diff(LineChange.DiffEnum.DO_NOTHING)
                                         .lineNumber(LINE_NUMBER)
                                         .line(INPUT_LINE)
                                         .build();

        final var change = lineChangeManager.applyChange(lineChange);

        StepVerifier.create(change)
                    .expectNext(INPUT_LINE)
                    .expectComplete()
                    .verify();
    }

    @Test
    void applyChangeShouldThrowBadLineChangeExceptionWhenHasViolation() {
        final var lineChange = lineChange(LineChange.DiffEnum.ERROR, "value", "value").build();
        final var expectedExceptionMessage = "too bad";
        lineChange.setViolation(expectedExceptionMessage);

        StepVerifier.create(lineChangeManager.applyChange(lineChange))
                    .expectErrorSatisfies(e -> assertThat(e).isExactlyInstanceOf(BadLineChangeException.class)
                                                            .hasMessageContaining(expectedExceptionMessage)
                                                            .extracting(ee -> ((BadLineChangeException) ee).getLineChange())
                                                            .isEqualTo(lineChange))
                    .verify()
        ;
    }

    @Test
    void applyChangeShouldReturnSameLineWhenEnabledWithSameValue() {
        final var lineChange = lineChange(LineChange.DiffEnum.DO_NOTHING, CONSTANT_VALUE, CONSTANT_VALUE).build();

        final var outputLine = lineChangeManager.applyChange(lineChange);

        StepVerifier.create(outputLine)
                    .expectNext(INPUT_LINE)
                    .expectComplete()
                    .verify();
    }

    @Test
    void applyChangeShouldReturnExpectedLineWhenWantedToBeDisabled() {
        final var lineChange = lineChange(LineChange.DiffEnum.TO_DISABLE, CONSTANT_VALUE, null).build();
        when(constantLineInterpreterMock.disableLine(lineChange.getConstantLineDetails())).thenReturn(Mono.just(OUTPUT_LINE));

        final var outputLine = lineChangeManager.applyChange(lineChange);

        StepVerifier.create(outputLine)
                    .expectNext(OUTPUT_LINE)
                    .expectComplete()
                    .verify();
    }

    @Test
    void applyChangeShouldReturnExpectedLineWhenWantedToBeEnabled() {
        final var lineChange = lineChange(LineChange.DiffEnum.TO_ENABLE, CONSTANT_VALUE, CONSTANT_VALUE).build();
        when(constantLineInterpreterMock.enableLine(lineChange.getConstantLineDetails())).thenReturn(Mono.just(OUTPUT_LINE));

        final var outputLine = lineChangeManager.applyChange(lineChange);

        StepVerifier.create(outputLine)
                    .expectNext(OUTPUT_LINE)
                    .expectComplete()
                    .verify();
    }

    @Test
    void applyChangeShouldReturnExpectedLineWhenWantedToBeEnabledWithOtherValue() {
        final var lineChange = lineChange(LineChange.DiffEnum.TO_ENABLE_AND_CHANGE_VALUE, CONSTANT_VALUE, CONSTANT_OTHER_VALUE).build();
        when(constantLineInterpreterMock.enableLineAndChangeValue(lineChange.getConstantLineDetails(), CONSTANT_OTHER_VALUE)).thenReturn(Mono.just(OUTPUT_LINE));

        final var outputLine = lineChangeManager.applyChange(lineChange);

        StepVerifier.create(outputLine)
                    .expectNext(OUTPUT_LINE)
                    .expectComplete()
                    .verify();
    }

    @Test
    void applyChangeShouldReturnExpectedLineWhenAlreadyEnabledButOtherValue() {
        final var lineChange = lineChange(LineChange.DiffEnum.CHANGE_VALUE, CONSTANT_VALUE, CONSTANT_OTHER_VALUE).build();
        when(constantLineInterpreterMock.changeValue(lineChange.getConstantLineDetails(), CONSTANT_OTHER_VALUE)).thenReturn(Mono.just(OUTPUT_LINE));

        final var outputLine = lineChangeManager.applyChange(lineChange);

        StepVerifier.create(outputLine)
                    .expectNext(OUTPUT_LINE)
                    .expectComplete()
                    .verify();
    }

    private LineChange.LineChangeBuilder lineChange(final LineChange.DiffEnum diff, final String oldValue, final String wantedValue) {
        return lineChange(CONSTANT_NAME, diff, oldValue, wantedValue);
    }

    private LineChange.LineChangeBuilder lineChange(final String constantName, final LineChange.DiffEnum diff, final String oldValue, final String wantedValue) {
        return LineChange.builder()
                         .line(INPUT_LINE)
                         .lineNumber(LINE_NUMBER)
                         .diff(diff)
                         .constant(LineChange.LineChangeConstant.builder()
                                                                .name(constantName)
                                                                .currentValue(oldValue)
                                                                .wantedValue(wantedValue)
                                                                .build())
                ;
    }

    private ConstantLineInterpreter.ParsedConstant parsedConstant(final Constant constant) {
        return ConstantLineInterpreter.ParsedConstant.builder()
                                                     .constant(constant)
                                                     .constantLineDetails(ConstantLineDetails.builder()
                                                                                             .line(INPUT_LINE)
                                                                                             .build())
                                                     .build();
    }
}
