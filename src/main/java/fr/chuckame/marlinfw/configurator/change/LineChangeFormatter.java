package fr.chuckame.marlinfw.configurator.change;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class LineChangeFormatter {
    private static final String DISABLE =
            "Disable        ";
    private static final String ENABLE =
            "Enable         ";
    private static final String CHANGE =
            "Change         ";
    private static final String ERROR =
            "Error          ";
    private static final String ENABLE_AND_CHANGE =
            "Enable & Change";

    private static final Map<LineChange.DiffEnum, String> FORMAT_DIFF_MAPPING = Map.of(
            LineChange.DiffEnum.TO_DISABLE, DISABLE,
            LineChange.DiffEnum.TO_ENABLE, ENABLE,
            LineChange.DiffEnum.TO_ENABLE_AND_CHANGE_VALUE, ENABLE_AND_CHANGE,
            LineChange.DiffEnum.CHANGE_VALUE, CHANGE,
            LineChange.DiffEnum.ERROR, ERROR
    );

    /**
     * To format like CONSTANT: E&C 15 → 18<br>
     */
    public String format(final LineChange lineChange) {
        if (!lineChange.isConstant()) {
            return null;
        }
        final var output = new StringBuilder();
        if (FORMAT_DIFF_MAPPING.containsKey(lineChange.getDiff())) {
            output.append(FORMAT_DIFF_MAPPING.get(lineChange.getDiff()));
            output.append(" ");
        }
        output.append(lineChange.getConstant().getName());
        switch (lineChange.getDiff()) {
            case DO_NOTHING:
                output.append(": Nothing to do");
                break;
            case TO_DISABLE:
            case TO_ENABLE:
                break;
            case TO_ENABLE_AND_CHANGE_VALUE:
            case CHANGE_VALUE:
                output.append(": ");
                formatValue(lineChange.getConstant().getCurrentValue(), output);
                output.append(" → ");
                formatValue(lineChange.getConstant().getWantedValue(), output);
                break;
            case ERROR:
                output.append(": ");
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
