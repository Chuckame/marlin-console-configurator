package fr.chuckame.marlinfw.configurator.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineParser;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ProfilePropertiesParserTest {
    public static final Path UNKNOWN_FILE_PATH = Path.of("unknownFile");
    public static final Path FILE_PATH_1 = Path.of("file1");
    public static final Path FILE_PATH_2 = Path.of("file2");
    public static final String CONSTANT_1_NAME = "1";
    public static final String CONSTANT_1_VALUE = "11";
    @Mock
    private ConstantLineParser constantLineParserMock;
    @Mock
    private FileHelper fileHelperMock;
    @InjectMocks
    private ProfilePropertiesParser profilePropertiesParser;

    @Test
    void parseFromFileShouldReturnExpectedProfile() throws IOException {
        final var profileFilePath = toPath("example/profile.yaml");
        Mockito.when(fileHelperMock.bytes(profileFilePath)).thenReturn(Mono.just(java.nio.file.Files.readAllBytes(profileFilePath)));

        final var profileProperties = profilePropertiesParser.parseFromFile(profileFilePath);

        assertThat(profileProperties.block()).isEqualTo(profileProperties());
    }

    @Test
    void writeToFileShouldWriteExpectedContent() throws IOException {
        final var outputFilePath = Path.of("coucou");
        final var yamlMapper = new ObjectMapper(new YAMLFactory());
        final var captor = ArgumentCaptor.forClass(byte[].class);
        Mockito.when(fileHelperMock.write(captor.capture(), ArgumentMatchers.same(outputFilePath))).thenReturn(Mono.empty());

        profilePropertiesParser.writeToFile(profileProperties(), outputFilePath).blockOptional();
        final ProfileProperties writtenProfileProperties = yamlMapper.readValue(captor.getValue(), ProfileProperties.class);

        assertThat(writtenProfileProperties).isEqualTo(profileProperties());
    }

    @Test
    void extractFromConstantsFilesShouldThrowExceptionWhenConfigurationFilesNotExisting() {
        final var files = List.of(UNKNOWN_FILE_PATH);
        final var exceptionThrown = new UncheckedIOException(new NoSuchFileException(UNKNOWN_FILE_PATH.toString()));
        Mockito.when(fileHelperMock.lines(UNKNOWN_FILE_PATH)).thenReturn(Flux.error(exceptionThrown));

        assertThatThrownBy(() -> profilePropertiesParser.extractFromConstantsFiles(files).block())
                .isExactlyInstanceOf(UncheckedIOException.class)
                .hasCauseExactlyInstanceOf(NoSuchFileException.class);
    }

    @Test
    void extractFromConfigurationFilesShouldReturnExpectedProfile() {
        final var files = List.of(FILE_PATH_1, FILE_PATH_2);
        final var expectedProfile = ProfileProperties.builder()
                                                     .enabled(Map.of(CONSTANT_1_NAME, CONSTANT_1_VALUE))
                                                     .disabled(List.of("2"))
                                                     .build();
        Mockito.when(fileHelperMock.lines(FILE_PATH_1)).thenReturn(Flux.just("line1"));
        Mockito.when(fileHelperMock.lines(FILE_PATH_2)).thenReturn(Flux.just("line2"));
        Mockito.when(constantLineParserMock.parseLine("line1")).thenReturn(Mono.just(Constant.builder().enabled(true).name(CONSTANT_1_NAME).value(CONSTANT_1_VALUE).build()));
        Mockito.when(constantLineParserMock.parseLine("line2")).thenReturn(Mono.just(Constant.builder().enabled(false).name("2").build()));

        final var extractedProfile = profilePropertiesParser.extractFromConstantsFiles(files);

        assertThat(extractedProfile.block()).isEqualTo(expectedProfile);
    }

    private Map<String, String> enabledConstants() {
        final Map<String, String> enabledConstants = new HashMap<>(Map.of(
                "CONSTANT_WITH_VALUE_NUMBER", "1",
                "CONSTANT_WITH_VALUE_STRING", "\"a string value that requires quotes\"",
                "CONSTANT_WITH_VALUE_COMPLEX_STRING", "\"a complex value that requires quotes and\\nreturns\"",
                "CONSTANT_WITH_VALUE_ARRAY", "{ 14, 15, 28 }",
                "WeiRd_conStANt", "hey"
        ));
        enabledConstants.put("CONSTANT_WITHOUT_VALUE", null);
        return enabledConstants;
    }

    private Map<String, String> disabledConstants() {
        final var constants = new LinkedHashMap<String, String>();
        constants.put("DISABLED_CONSTANT", null);
        constants.put("DiSaBLeD_CoNsTANt2", null);
        return constants;
    }

    private ProfileProperties profileProperties() {
        return ProfileProperties.builder()
                                .enabled(enabledConstants())
                                .disabled(new ArrayList<>(disabledConstants().keySet()))
                                .build();
    }


    private Path toPath(final String resourcePath) {
        return new File(Objects.requireNonNull(getClass().getClassLoader().getResource(resourcePath), resourcePath + " not found").getFile()).toPath();
    }
}
