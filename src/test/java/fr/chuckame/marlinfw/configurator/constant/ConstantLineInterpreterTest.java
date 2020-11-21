package fr.chuckame.marlinfw.configurator.constant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantLineInterpreterTest {
    private final ConstantLineInterpreter constantLineInterpreter = new ConstantLineInterpreter();

    // fixme cannot parse following lines when first line ending with backslash '\': https://github.com/Chuckame/marlin-console-configurator/issues/1
    @Test
    void TEMPORARY_FIX_parseLineShouldReturnNothingWhenValueEndsWithBackslash() {
        final var line = "#define CONSTANT value \\";

        final var constant = constantLineInterpreter.parseLine(line);

        assertThat(constant.blockOptional()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "'#define   coNsTANT   ',coNsTANT,",
            "'#define HEADER    azert',HEADER,azert",
            "'#define A_VALUE 123',A_VALUE,123",
            "'#define coNsTANT       123    ',coNsTANT,123",
            "'          #define cool_CONSTANT       123    ',cool_CONSTANT,123",
            "'          #define cool_CONSTANT       { 15, 23, 88 }    ',cool_CONSTANT,'{ 15, 23, 88 }'",
            "'        #define coNsTANT       \"quoted text\"    ',coNsTANT,\"quoted text\"",
            "'        #define coNsTANT       \"quoted\\n text\"    ',coNsTANT,\"quoted\\n text\"",
    })
    void parseLineShouldReturnExpectedNameAndValueAndEnabled(final String line, final String expectedName, final String expectedValue) {
        final var expectedConstant = Constant.builder().enabled(true).name(expectedName).value(expectedValue).build();

        final var constant = constantLineInterpreter.parseLine(line);

        assertThat(constant.blockOptional()).map(ConstantLineInterpreter.ParsedConstant::getConstant).hasValue(expectedConstant);
    }

    @ParameterizedTest
    @CsvSource({
            "'//#define   coNsTANT   ',coNsTANT,",
            "' // #define HEADER    azert',HEADER,azert",
            "'//   #define A_VALUE 123',A_VALUE,123",
            "'   // #define coNsTANT       123    ',coNsTANT,123",
            "'  //      #define cool_CONSTANT       { 15, 23, 88 }    ',cool_CONSTANT,'{ 15, 23, 88 }'",
            "' //         #define cool_CONSTANT       123    ',cool_CONSTANT,123",
            "'        //#define coNsTANT       \"quoted\\n text\"    ',coNsTANT,\"quoted\\n text\"",
    })
    void parseLineShouldReturnExpectedNameAndValueAndDisabled(final String line, final String expectedName, final String expectedValue) {
        final var expectedConstant = Constant.builder().enabled(false).name(expectedName).value(expectedValue).build();

        final var constant = constantLineInterpreter.parseLine(line);

        assertThat(constant.blockOptional()).map(ConstantLineInterpreter.ParsedConstant::getConstant).hasValue(expectedConstant);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " * #define   coNsTANT  ",
            "",
            "   some text  ",
    })
    void parseLineShouldReturnNothing(final String line) {
        final var constant = constantLineInterpreter.parseLine(line);

        assertThat(constant.blockOptional()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "#define   coNsTANT   ",
            "#define HEADER    azert",
            "#define A_VALUE 123",
            "#define coNsTANT       123    ",
            "          #define cool_CONSTANT       123    ",
            "          #define cool_CONSTANT       { 15, 23, 88 }    ",
            "        #define coNsTANT       \"quoted text\"    ",
            "        #define coNsTANT       \"quoted\\n text\"    ",
    })
    void disableLine(final String line) {
        final var lineDetails = ConstantLineDetails.builder()
                                                   .line(line)
                                                   .build();

        final var constant = constantLineInterpreter.disableLine(lineDetails);

        assertThat(constant.blockOptional()).hasValue("//" + line);
    }

    @ParameterizedTest
    @CsvSource({
            "'// #define   coNsTANT   ',0,2,' #define   coNsTANT   '",
            "'   //#define HEADER    azert',3,5,'   #define HEADER    azert'",
            "'  //   #define A_VALUE 123',2,4,'     #define A_VALUE 123'",
            "'//        #define coNsTANT       123    ',0,2,'        #define coNsTANT       123    '",
            "'         // #define cool_CONSTANT       123    ',9,11,'          #define cool_CONSTANT       123    '",
            "'     //     #define cool_CONSTANT       { 15, 23, 88 }    ',5,7,'          #define cool_CONSTANT       { 15, 23, 88 }    '",
            "'        //#define coNsTANT       \"quoted\\n text\"    ',8,10,'        #define coNsTANT       \"quoted\\n text\"    '",
    })
    void enableLine(final String line, final int startIndex, final int endIndex, final String expectedLine) {
        final var lineDetails = ConstantLineDetails.builder()
                                                   .line(line)
                                                   .disabledMatchIndex(ConstantLineDetails.MatchIndex.builder()
                                                                                                     .start(startIndex)
                                                                                                     .end(endIndex)
                                                                                                     .build())
                                                   .build();

        final var constant = constantLineInterpreter.enableLine(lineDetails);

        assertThat(constant.blockOptional()).hasValue(expectedLine);
    }

    @ParameterizedTest
    @CsvSource({
            "'   //#define HEADER    azert',3,5,23,28,toto,'   #define HEADER    toto'",
            "'  //   #define A_VALUE 123',2,4,23,26,OH YEAH,'     #define A_VALUE OH YEAH'",
            "'//        #define coNsTANT       123    ',0,2,33,36,Some value,'        #define coNsTANT       Some value    '",
            "'         // #define cool_CONSTANT       123    ',9,11,40,43,azerty1234§è!çà,'          #define cool_CONSTANT       azerty1234§è!çà    '",
            "'     //     #define cool_CONSTANT       { 15, 23, 88 }    ',5,7,40,54,'{ 111, 222, 333 }','          #define cool_CONSTANT       { 111, 222, 333 }    '",
            "'        //#define coNsTANT       \"quoted\\n text\"    ',8,10,33,48,\"other quoted\\n text cool\",'        #define coNsTANT       \"other quoted\\n text cool\"    '",
    })
    void enableLineAndChangeValue(final String line, final int disabledStart, final int disabledEnd, final int valueStart, final int valueEnd, final String newValue,
                                  final String expectedLine) {
        final var lineDetails = ConstantLineDetails.builder()
                                                   .line(line)
                                                   .disabledMatchIndex(ConstantLineDetails.MatchIndex.builder()
                                                                                                     .start(disabledStart)
                                                                                                     .end(disabledEnd)
                                                                                                     .build())
                                                   .valueMatchIndex(ConstantLineDetails.MatchIndex.builder()
                                                                                                  .start(valueStart)
                                                                                                  .end(valueEnd)
                                                                                                  .build())
                                                   .build();

        final var constant = constantLineInterpreter.enableLineAndChangeValue(lineDetails, newValue);

        assertThat(constant.blockOptional()).hasValue(expectedLine);
    }

    @ParameterizedTest
    @CsvSource({
            "'   #define HEADER    azert',21,26,toto,'   #define HEADER    toto'",
            "'     #define A_VALUE 123',21,24,OH YEAH,'     #define A_VALUE OH YEAH'",
            "'        #define coNsTANT       123    ',31,34,Some value,'        #define coNsTANT       Some value    '",
            "'          #define cool_CONSTANT       123    ',38,41,azerty1234§è!çà,'          #define cool_CONSTANT       azerty1234§è!çà    '",
            "'          #define cool_CONSTANT       { 15, 23, 88 }    ',38,52,'{ 111, 222, 333 }','          #define cool_CONSTANT       { 111, 222, 333 }    '",
            "'        #define coNsTANT       \"quoted\\n text\"    ',31,46,\"other quoted\\n text cool\",'        #define coNsTANT       \"other quoted\\n text cool\"    '",
    })
    void changeValue(final String line, final int startIndex, final int endIndex, final String newValue, final String expectedLine) {
        final var lineDetails = ConstantLineDetails.builder()
                                                   .line(line)
                                                   .valueMatchIndex(ConstantLineDetails.MatchIndex.builder()
                                                                                                  .start(startIndex)
                                                                                                  .end(endIndex)
                                                                                                  .build())
                                                   .build();

        final var constant = constantLineInterpreter.changeValue(lineDetails, newValue);

        assertThat(constant.blockOptional()).hasValue(expectedLine);
    }
}
