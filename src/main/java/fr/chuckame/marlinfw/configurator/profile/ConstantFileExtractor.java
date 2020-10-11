package fr.chuckame.marlinfw.configurator.profile;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineParser;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@RequiredArgsConstructor
public class ConstantFileExtractor {
    private final ConstantLineParser constantLineParser;
    private final FileHelper fileHelper;

    public Mono<ProfileProperties> extractFromConstantsFile(final Path filePath) {
        return fileHelper.lines(filePath)
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
}
