package fr.chuckame.marlinfw.configurator.constant;

import fr.chuckame.marlinfw.configurator.profile.ProfileProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProfilePropertiesChangeAdapter {
    public Map<String, Constant> getWantedConstants(final ProfileProperties profileProperties) {
        final var wantedChanges = new HashMap<String, Constant>();
        profileProperties.getDisabled().forEach(constantName -> wantedChanges.put(constantName, disabledConstant(constantName)));
        profileProperties.getEnabled().forEach((constantName, constantValue) -> wantedChanges.put(constantName, enabledConstant(constantName, constantValue)));
        return wantedChanges;
    }

    private Constant disabledConstant(final String name) {
        return Constant.builder().name(name).enabled(false).build();
    }

    private Constant enabledConstant(final String name, final String value) {
        return Constant.builder().name(name).value(value).enabled(true).build();
    }
}
