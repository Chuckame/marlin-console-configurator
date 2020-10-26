package fr.chuckame.marlinfw.configurator.change;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class LineChangeFormatter {
    private static final char DISABLE = 'D';
    private static final char ENABLE = 'E';
    private static final char CHANGE = 'C';
    private static final String ENABLE_AND_CHANGE = String.format("%s&%s", ENABLE, CHANGE);

    /**
     * To format like CONSTANT: E&C 15 → 18<br>
     */
    public String format(final LineChange lineChange) {
        if (!lineChange.isConstant()) {
            return null;
        }
        final var output = new StringBuilder();
        output.append(lineChange.getConstant().getName());
        output.append(": ");
        switch (lineChange.getDiff()) {
            case DO_NOTHING:
                output.append("Nothing to do");
                break;
            case TO_DISABLE:
                output.append(DISABLE);
                break;
            case TO_ENABLE:
                output.append(ENABLE);
                break;
            case TO_ENABLE_AND_CHANGE_VALUE:
                output.append(ENABLE_AND_CHANGE);
                output.append(' ');
                formatValue(lineChange.getConstant().getCurrentValue(), output);
                output.append(" → ");
                formatValue(lineChange.getConstant().getWantedValue(), output);
                break;
            case CHANGE_VALUE:
                output.append(CHANGE);
                output.append(' ');
                formatValue(lineChange.getConstant().getCurrentValue(), output);
                output.append(" → ");
                formatValue(lineChange.getConstant().getWantedValue(), output);
                break;
            case ERROR:
                output.append("ERROR ");
                output.append(lineChange.getViolation());
                break;
            default:
                throw new UnsupportedOperationException("Unknown diff: " + lineChange.getDiff());
        }
        return output.toString();
    }

    private void formatValue(@Nullable final String value, final StringBuilder output) {
        if (StringUtils.hasText(value)) {
            output.append(value);
        }
    }
}
