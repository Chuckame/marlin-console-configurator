package fr.chuckame.marlinfw.configurator.profile;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ProfileProperties {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Map<String, String> enabled;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> disabled;
}
