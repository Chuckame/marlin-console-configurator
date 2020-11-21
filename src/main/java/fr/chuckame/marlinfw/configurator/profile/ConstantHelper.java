package fr.chuckame.marlinfw.configurator.profile;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ConstantLineInterpreter;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConstantHelper {
    private final FileHelper fileHelper;
    private final ConstantLineInterpreter constantLineInterpreter;

    public Mono<ProfileProperties> constantsToProfile(final Flux<Constant> constants) {
        return constants.reduceWith(this::initEmptyProfile, this::addConstantToProfile);
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

    public Flux<Constant> getConstants(final List<Path> files) {
        return fileHelper.listFiles(files)
                         .flatMap(fileHelper::lines)
                         .flatMap(constantLineInterpreter::parseLine)
                         .map(ConstantLineInterpreter.ParsedConstant::getConstant);
    }
}
