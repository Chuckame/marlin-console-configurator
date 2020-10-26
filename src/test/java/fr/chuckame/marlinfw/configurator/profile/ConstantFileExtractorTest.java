package fr.chuckame.marlinfw.configurator.profile;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ConstantFileExtractorTest {
    private static final Path FILE_PATH = Path.of("a-file.ext");
    private static final String CONSTANT_NAME = "a-constant";
    private static final String CONSTANT_VALUE = "11";
    private static final String DISABLED_CONSTANT_NAME = "2";
    private static final String LINE_1 = "line1";
    private static final String LINE_2 = "line2";

    @Mock
    private ConstantLineInterpreter constantLineInterpreterMock;
    @Mock
    private FileHelper fileHelperMock;
    @InjectMocks
    private ConstantFileExtractor constantFileExtractor;

    @Test
    void extractFromConstantsFileShouldThrowExceptionWhenConfigurationFilesNotExisting() {
        final var exceptionThrown = new UncheckedIOException(new NoSuchFileException(FILE_PATH.toString()));
        Mockito.when(fileHelperMock.lines(FILE_PATH)).thenReturn(Flux.error(exceptionThrown));

        assertThatThrownBy(() -> constantFileExtractor.extractFromConstantsFile(FILE_PATH).block())
                .isExactlyInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(NoSuchFileException.class);
    }

    @Test
    void extractFromConstantsFileShouldReturnExpectedProfile() {
        final var expectedProfile = ProfileProperties.builder()
                                                     .enabled(Map.of(CONSTANT_NAME, CONSTANT_VALUE))
                                                     .disabled(List.of(DISABLED_CONSTANT_NAME))
                                                     .build();
        Mockito.when(fileHelperMock.lines(FILE_PATH)).thenReturn(Flux.just(LINE_1, LINE_2));
        Mockito.when(constantLineInterpreterMock.parseLine(LINE_1))
               .thenReturn(Mono.just(ConstantLineInterpreter.ParsedConstant.builder().constant(Constant.builder().enabled(true).name(CONSTANT_NAME).value(CONSTANT_VALUE).build())
                                                                           .build()));
        Mockito.when(constantLineInterpreterMock.parseLine(LINE_2))
               .thenReturn(Mono.just(ConstantLineInterpreter.ParsedConstant.builder().constant(Constant.builder().enabled(false).name(DISABLED_CONSTANT_NAME).build()).build()));

        final var extractedProfile = constantFileExtractor.extractFromConstantsFile(FILE_PATH);

        assertThat(extractedProfile.block()).isEqualTo(expectedProfile);
    }

}
