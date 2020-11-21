package fr.chuckame.marlinfw.configurator.constant;

import fr.chuckame.marlinfw.configurator.profile.ProfileProperties;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAdapterTest {
    private final ProfileAdapter profileAdapter = new ProfileAdapter();

    @Test
    void getWantedConstantsShouldReturnEmptyMapWhenEmptyEnabledAndEmptyDisabled() {
        final var profile = ProfileProperties.builder()
                                             .enabled(Map.of())
                                             .disabled(List.of())
                                             .build();

        final var wantedConstants = profileAdapter.profileToConstants(profile);

        assertThat(wantedConstants).isEmpty();
    }

    @Test
    void getWantedConstantsShouldReturnExpectedConstants() {
        final var profile = ProfileProperties.builder()
                                             .enabled(withNullValue("c1", Map.of("c2", "v2")))
                                             .disabled(List.of("c3", "c4"))
                                             .build();

        final var wantedConstants = profileAdapter.profileToConstants(profile);

        assertThat(wantedConstants)
                .containsEntry("c1", Constant.builder().enabled(true).name("c1").value(null).build())
                .containsEntry("c2", Constant.builder().enabled(true).name("c2").value("v2").build())
                .containsEntry("c3", Constant.builder().enabled(false).name("c3").value(null).build())
                .containsEntry("c4", Constant.builder().enabled(false).name("c4").value(null).build());
    }

    private Map<String, String> withNullValue(final String key, final Map<String, String> map) {
        final var output = new HashMap<>(map);
        output.put(key, null);
        return output;
    }
}
