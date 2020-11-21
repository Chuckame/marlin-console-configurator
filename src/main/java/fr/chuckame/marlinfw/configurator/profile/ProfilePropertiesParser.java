package fr.chuckame.marlinfw.configurator.profile;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import fr.chuckame.marlinfw.configurator.util.ExceptionUtils;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.INDENT_ARRAYS;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.SPLIT_LINES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

@Component
@RequiredArgsConstructor
public class ProfilePropertiesParser {
    private final ObjectMapper yamlParser = prepareYamlMapper();
    private final FileHelper fileHelper;

    public Mono<ProfileProperties> parseFromFiles(final List<Path> profileFilePaths) {
        return fileHelper.listFiles(profileFilePaths)
                         .flatMap(this::parseFromFile)
                         .reduceWith(ProfileProperties::new, ProfileProperties::merge);
    }

    public Mono<ProfileProperties> parseFromFile(final Path profileFilePath) {
        return fileHelper.read(profileFilePath)
                         .map(ExceptionUtils.wrap(bytes -> yamlParser.readValue(bytes, ProfileProperties.class)));
    }

    public Mono<Void> writeToFile(final ProfileProperties profile, final Path outputFilePath) {
        return Mono.fromCallable(() -> yamlParser.writeValueAsBytes(profile))
                   .flatMap(bytes -> fileHelper.write(bytes, outputFilePath))
                   .then();
    }

    public Mono<String> writeToString(final ProfileProperties profile) {
        return Mono.fromCallable(() -> yamlParser.writeValueAsString(profile));
    }

    private ObjectMapper prepareYamlMapper() {
        return new ObjectMapper(newYamlFactoryWithCustomIndentationForArrays(2)
                                        .enable(MINIMIZE_QUOTES)
                                        .enable(INDENT_ARRAYS)
                                        .disable(SPLIT_LINES)
                                        .disable(WRITE_DOC_START_MARKER));
    }

    private YAMLFactory newYamlFactoryWithCustomIndentationForArrays(final int indentation) {
        return new YAMLFactory() {
            @Override
            protected YAMLGenerator _createGenerator(final Writer out, final IOContext ctxt) throws IOException {
                return new YAMLGenerator(ctxt, _generatorFeatures, _yamlGeneratorFeatures,
                                         _objectCodec, out, _version) {
                    @Override
                    protected DumperOptions buildDumperOptions(final int jsonFeatures, final int yamlFeatures, final DumperOptions.Version version) {
                        final var opts = super.buildDumperOptions(jsonFeatures, yamlFeatures, version);
                        opts.setIndicatorIndent(indentation);
                        return opts;
                    }

                    @Override
                    public void writeNull() throws IOException {
                        _verifyValueWrite("write null value");
                        // no real type for this, is there?
                        _writeScalar("", "object", DumperOptions.ScalarStyle.PLAIN);
                    }
                };
            }
        };
    }
}
