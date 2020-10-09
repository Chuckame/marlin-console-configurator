package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.Objects;

@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LineChange {
    private final String line;
    private final int lineNumber;
    @Nullable
    private Constant parsedConstant;
    @Nullable
    private Constant wantedConstant;
    private EnabledDiffEnum enabledDiff;

    LineChange(@NonNull final String line, final int lineNumber) {
        this.line = line;
        this.lineNumber = lineNumber;
        enabledDiff = EnabledDiffEnum.DO_NOTHING;
    }

    @Builder(access = AccessLevel.PACKAGE)
    private LineChange(@NonNull final String line, @NonNull final Integer lineNumber, @Nullable final Constant parsedConstant, @Nullable final Constant wantedConstant,
                       @NonNull final EnabledDiffEnum enabledDiff) {
        this.line = line;
        this.lineNumber = lineNumber;
        this.parsedConstant = parsedConstant;
        this.wantedConstant = wantedConstant;
        this.enabledDiff = enabledDiff;
    }

    public enum EnabledDiffEnum {
        DO_NOTHING,
        TO_ENABLE,
        TO_DISABLE
    }

    public boolean isConstant() {
        return parsedConstant != null;
    }

    public boolean hasWantedConstant() {
        return wantedConstant != null;
    }

    public boolean isValueChanged() {
        return isConstant() && hasWantedConstant() && !Objects.equals(parsedConstant.getValue(), wantedConstant.getValue());
    }
}
