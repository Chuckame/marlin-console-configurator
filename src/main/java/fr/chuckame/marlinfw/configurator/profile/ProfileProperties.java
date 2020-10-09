package fr.chuckame.marlinfw.configurator.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ProfileProperties {
    private Map<String, String> enabled;
    private List<String> disabled;
}
