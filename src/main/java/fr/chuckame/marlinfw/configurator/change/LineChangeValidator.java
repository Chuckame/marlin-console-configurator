package fr.chuckame.marlinfw.configurator.change;

import fr.chuckame.marlinfw.configurator.constant.Constant;

public class LineChangeValidator {
    private static final String MISSING_WANTED_VALUE_MESSAGE = "Wanted value should be defined";
    private static final String MISSING_PARSED_VALUE_MESSAGE = "Wanted value should not be defined";

    public String getViolation(final Constant parsed, final Constant wanted) {
        if (parsed == null || wanted == null) {
            return null;
        }
        if (wanted.isEnabled()) {
            if (parsed.getValue() != null && wanted.getValue() == null) {
                return MISSING_WANTED_VALUE_MESSAGE;
            }
            if (parsed.getValue() == null && wanted.getValue() != null) {
                return MISSING_PARSED_VALUE_MESSAGE;
            }
        }
        return null;
    }
}
