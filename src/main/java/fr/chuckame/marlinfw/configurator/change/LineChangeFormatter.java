package fr.chuckame.marlinfw.configurator.change;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;

public class LineChangeFormatter {
    private static final char DISABLE = 'D';
    private static final char ENABLE = 'E';
    private static final char CHANGE = 'C';

    /**
     * To format like CONSTANT: E&C 15 → 18<br>
     */
    public String format(final LineChange lineChange) {
        if (lineChange.getParsedConstant() == null) {
            return null;
        }
        final var before = lineChange.getParsedConstant();
        final var after = lineChange.getWantedConstant();
        final var output = new StringBuilder();
        output.append(before.getName());
        output.append(": ");
        if (!lineChange.hasWantedConstant() || Objects.equals(before.getValue(), after.getValue()) && lineChange.getEnabledDiff() == LineChange.EnabledDiffEnum.DO_NOTHING) {
            output.append("Nothing to do");
        } else if (lineChange.getEnabledDiff() == LineChange.EnabledDiffEnum.TO_DISABLE) {
            output.append(DISABLE);
        } else {
            if (lineChange.getEnabledDiff() == LineChange.EnabledDiffEnum.TO_ENABLE) {
                output.append(ENABLE);
            }
            if (!Objects.equals(before.getValue(), after.getValue())) {
                if (lineChange.getEnabledDiff() != LineChange.EnabledDiffEnum.DO_NOTHING) {
                    output.append('&');
                }
                output.append(CHANGE);
                output.append(' ');
                formatValue(before.getValue(), output);
                output.append(" → ");
                formatValue(after.getValue(), output);
            }
        }

        return output.toString();
    }

    private void formatValue(@Nullable final String value, final StringBuilder output) {
        if (StringUtils.hasText(value)) {
            output.append(value);
        } else {
            output.append("<no value>");
        }
    }
}
