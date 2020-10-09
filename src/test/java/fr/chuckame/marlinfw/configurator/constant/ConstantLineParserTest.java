package fr.chuckame.marlinfw.configurator.constant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantLineParserTest {
    private final ConstantLineParser constantLineParser = new ConstantLineParser();

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
        final var expectedConstant = Constant.builder().enabled(true).name(expectedName).value(expectedValue).comment(null).build();

        final var constant = constantLineParser.parseLine(line);

        assertThat(constant.blockOptional()).hasValue(expectedConstant);
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
        final var expectedConstant = Constant.builder().enabled(false).name(expectedName).value(expectedValue).comment(null).build();

        final var constant = constantLineParser.parseLine(line);

        assertThat(constant.blockOptional()).hasValue(expectedConstant);
    }

    @ParameterizedTest
    @CsvSource({
            "'//   #define A_VALUE 123   // a comment',a comment",
            "'   // #define coNsTANT       123  //   a comment  ',a comment",
            "'  //      #define cool_CONSTANT       { 15, 23, 88 }  // great comment  ','great comment'",
            "' //         #define cool_CONSTANT       123   // holà comment ',holà comment",
            "'        //#define coNsTANT       \"quoted\\n text\"   // an \"other\" comment ','an \"other\" comment'",
    })
    void parseLineShouldReturnExpectedComment(final String line, final String expectedComment) {
        final var constant = constantLineParser.parseLine(line);

        assertThat(constant.blockOptional().map(Constant::getComment)).hasValue(expectedComment);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "//#define   coNsTANT   //",
            " // #define HEADER    azert ",
            "  //      #define cool_CONSTANT       { 15, 23, 88 }  //    ",
            " //         #define cool_CONSTANT       123    ",
            "        //#define coNsTANT       \"quoted\\n text\"   //  ",
    })
    void parseLineShouldReturnNoComment(final String line) {
        final var constant = constantLineParser.parseLine(line);

        assertThat(constant.blockOptional().map(Constant::getComment)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " * #define   coNsTANT  ",
            "",
            "   some text  ",
    })
    void parseLineShouldReturnNothing(final String line) {
        final var constant = constantLineParser.parseLine(line);

        assertThat(constant.blockOptional()).isEmpty();
    }
}
