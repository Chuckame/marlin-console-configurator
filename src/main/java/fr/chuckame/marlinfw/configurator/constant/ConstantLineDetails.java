package fr.chuckame.marlinfw.configurator.constant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConstantLineDetails {
    private final String line;
    private final MatchIndex disabledMatchIndex;
    private final MatchIndex valueMatchIndex;

    @Data
    @Builder
    public static class MatchIndex {
        private final int start;
        private final int end;
    }
}
