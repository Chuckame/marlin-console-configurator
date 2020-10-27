package fr.chuckame.marlinfw.configurator.profile;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
public class ConstantHelper {
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
}
