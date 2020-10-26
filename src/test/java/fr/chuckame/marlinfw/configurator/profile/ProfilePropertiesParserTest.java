package fr.chuckame.marlinfw.configurator.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProfilePropertiesParserTest {
    private final Path FILE_PATH = resourceToPath("profile.yaml");

    @Mock
    private FileHelper fileHelperMock;
    @InjectMocks
    private ProfilePropertiesParser profilePropertiesParser;

    @Test
    void parseFromFileShouldReturnExpectedProfile() throws IOException {
        Mockito.when(fileHelperMock.read(FILE_PATH)).thenReturn(Mono.just(Files.readAllBytes(FILE_PATH)));

        final var profileProperties = profilePropertiesParser.parseFromFile(FILE_PATH);

        StepVerifier.create(profileProperties)
                    .expectNext(profileProperties())
                    .expectComplete()
                    .verify();
    }

    @Test
    void parseFromFileShouldReturnNotReturnNullValues() throws IOException {
        Mockito.when(fileHelperMock.read(FILE_PATH)).thenReturn(Mono.just("enabled:\n".getBytes()));

        final var profileProperties = profilePropertiesParser.parseFromFile(FILE_PATH);

        StepVerifier.create(profileProperties)
                    .expectNext(ProfileProperties.builder()
                                                 .enabled(Map.of())
                                                 .disabled(List.of())
                                                 .build())
                    .expectComplete()
                    .verify();
    }

    @Test
    void writeToFileShouldWriteExpectedContent() throws IOException {
        final var yamlMapper = new ObjectMapper(new YAMLFactory());
        final var captor = ArgumentCaptor.forClass(byte[].class);
        Mockito.when(fileHelperMock.write(captor.capture(), ArgumentMatchers.same(FILE_PATH))).thenReturn(Mono.empty());

        final var writerMono = profilePropertiesParser.writeToFile(profileProperties(), FILE_PATH);

        StepVerifier.create(writerMono)
                    .expectComplete()
                    .verify();

        final ProfileProperties writtenProfileProperties = yamlMapper.readValue(captor.getValue(), ProfileProperties.class);
        assertThat(writtenProfileProperties).isEqualTo(profileProperties());
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

    private Path resourceToPath(final String resourcePath) {
        return new File(Objects.requireNonNull(getClass().getClassLoader().getResource(resourcePath), resourcePath + " not found").getFile()).toPath();
    }
}
