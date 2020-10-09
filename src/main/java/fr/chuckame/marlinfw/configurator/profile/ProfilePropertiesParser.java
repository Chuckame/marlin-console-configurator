package fr.chuckame.marlinfw.configurator.profile;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineParser;
import fr.chuckame.marlinfw.configurator.util.ExceptionUtils;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.DumperOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.INDENT_ARRAYS;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.SPLIT_LINES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

@RequiredArgsConstructor
public class ProfilePropertiesParser {
    private final ObjectMapper yamlParser = new ObjectMapper(newYamlFactoryWithCustomIndentationForArrays(2)
                                                                     .enable(MINIMIZE_QUOTES)
                                                                     .enable(INDENT_ARRAYS)
                                                                     .disable(SPLIT_LINES)
                                                                     .disable(WRITE_DOC_START_MARKER));
    private final ConstantLineParser constantLineParser;
    private final FileHelper fileHelper;

    public Mono<ProfileProperties> parseFromFile(final Path profileFilePath) {
        return fileHelper.bytes(profileFilePath)
                         .map(ExceptionUtils.wrap(bytes -> yamlParser.readValue(bytes, ProfileProperties.class)));
    }

    public Mono<Void> writeToFile(final ProfileProperties profile, final Path profileFilePath) {
        return Mono.fromCallable(() -> yamlParser.writeValueAsBytes(profile))
                   .flatMap(bytes -> fileHelper.write(bytes, profileFilePath))
                   .then();
    }

    public Mono<ProfileProperties> extractFromConstantsFiles(final List<Path> filesPaths) {
        return Flux.fromIterable(filesPaths)
                   .flatMap(fileHelper::lines)
                   .flatMap(constantLineParser::parseLine)
                   .reduceWith(this::initEmptyProfile, this::addConstantToProfile);
    }

    private ProfileProperties initEmptyProfile() {
        return ProfileProperties.builder().disabled(new ArrayList<>()).enabled(new LinkedHashMap<>()).build();
    }

    private ProfileProperties addConstantToProfile(final ProfileProperties profile, final Constant constant) {
        if (constant.isEnabled()) {
            profile.getEnabled().put(constant.getName(), constant.getValue());
        } else {
            profile.getDisabled().add(constant.getName());
        }
        return profile;
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
                };
            }
        };
    }
}
