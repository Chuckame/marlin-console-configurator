package fr.chuckame.marlinfw.configurator.constant;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConstantLineParser {
    private static final Pattern CONSTANT_REGEX = Pattern.compile("^(?:\\h*(//))?\\h*#define\\h+(\\w+)(?:\\h+([^/\\r\\n]+))?(?:\\h*//\\h*(.+))?$");

    public Mono<Constant> parseLine(final String line) {
        return Mono.just(line)
                   .map(CONSTANT_REGEX::matcher)
                   .filter(Matcher::matches)
                   .map(this::toConstant);
    }

    private Constant toConstant(final Matcher matcher) {
        return Constant.builder()
                       .enabled(matcher.group(1) == null)
                       .name(matcher.group(2))
                       .value(trim(matcher.group(3)))
                       .comment(trim(matcher.group(4)))
                       .build();
    }

    private String trim(@Nullable final String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
