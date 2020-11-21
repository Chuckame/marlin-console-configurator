package fr.chuckame.marlinfw.configurator.constant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
@Builder
@AllArgsConstructor
public class Constant {
    private boolean enabled;
    private String name;
    @Nullable
    private String value;
}
