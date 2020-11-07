package fr.chuckame.marlinfw.configurator.profile;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileProperties {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Map<String, String> enabled;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> disabled;

    public static ProfileProperties merge(final ProfileProperties a, final ProfileProperties b) {
        final var props = new ProfileProperties(new LinkedHashMap<>(), new ArrayList<>());
        if (a.enabled != null) {
            props.enabled.putAll(a.enabled);
        }
        if (a.disabled != null) {
            props.disabled.addAll(a.disabled);
        }
        if (b.enabled != null) {
            props.enabled.putAll(b.enabled);
        }
        if (b.disabled != null) {
            props.disabled.addAll(b.disabled);
        }
        return props;
    }
}
