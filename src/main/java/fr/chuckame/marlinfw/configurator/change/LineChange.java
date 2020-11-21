package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.ConstantLineDetails;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Data
@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LineChange {
    private final String line;
    private final int lineNumber;
    private final DiffEnum diff;
    @Nullable
    private final LineChangeConstant constant;
    @Nullable
    private final ConstantLineDetails constantLineDetails;
    @Nullable
    @Setter(AccessLevel.PACKAGE)
    private String violation;

    LineChange(final String line, final int lineNumber) {
        this.line = line;
        this.lineNumber = lineNumber;
        diff = DiffEnum.DO_NOTHING;
        constant = null;
        constantLineDetails = null;
        violation = null;
    }

    @Data
    @Builder
    public static class LineChangeConstant {
        private final String name;
        private final String currentValue;
        @Nullable
        private String wantedValue;
    }

    public enum DiffEnum {
        DO_NOTHING,
        TO_ENABLE,
        TO_ENABLE_AND_CHANGE_VALUE,
        CHANGE_VALUE,
        TO_DISABLE,
        ERROR
    }

    public boolean isConstant() {
        return constant != null;
    }
}
