package fr.chuckame.marlinfw.configurator.constant;

import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConstantLineInterpreter {
    private static final Pattern CONSTANT_REGEX = Pattern.compile("^(?:\\h*(//))?\\h*#define\\h+(\\w+)(?:\\h+([^/\\h]+(?:\\h+[^/\\h]+)*))?\\h*(?://\\h*(.*))?$");

    @Data
    @Builder
    public static class ParsedConstant {
        private final Constant constant;
        private final ConstantLineDetails constantLineDetails;
    }

    public Mono<ParsedConstant> parseLine(final String line) {
        return Mono.just(line)
                   .map(CONSTANT_REGEX::matcher)
                   .filter(Matcher::matches)
                   .map(this::toConstant);
    }

    public Mono<String> disableLine(final ConstantLineDetails constantLineDetails) {
        return Mono.just(constantLineDetails)
                   .map(ConstantLineDetails::getLine)
                   .map("//"::concat);
    }

    public Mono<String> enableLine(final ConstantLineDetails constantLineDetails) {
        return Mono.fromSupplier(() -> replace(constantLineDetails.getLine(), constantLineDetails.getDisabledMatchIndex(), ""));
    }

    public Mono<String> enableLineAndChangeValue(final ConstantLineDetails constantLineDetails, final String newValue) {
        return Mono.fromSupplier(() -> replace(constantLineDetails.getLine(),
                                               constantLineDetails.getDisabledMatchIndex(), "",
                                               constantLineDetails.getValueMatchIndex(), newValue));
    }

    public Mono<String> changeValue(final ConstantLineDetails constantLineDetails, final String newValue) {
        return Mono.fromSupplier(() -> replace(constantLineDetails.getLine(), constantLineDetails.getValueMatchIndex(), newValue));
    }

    private ParsedConstant toConstant(final Matcher matcher) {
        return ParsedConstant.builder()
                             .constant(Constant.builder()
                                               .enabled(matcher.group(1) == null)
                                               .name(matcher.group(2))
                                               .value(trim(matcher.group(3)))
                                               .comment(trim(matcher.group(4)))
                                               .build())
                             .constantLineDetails(ConstantLineDetails.builder()
                                                                     .line(matcher.group(0))
                                                                     .disabledMatchIndex(matchIndex(matcher, 1))
                                                                     .valueMatchIndex(matchIndex(matcher, 3))
                                                                     .build())
                             .build();
    }

    private ConstantLineDetails.MatchIndex matchIndex(final Matcher matcher, final int group) {
        if (matcher.group(group) == null) {
            return null;
        }
        return ConstantLineDetails.MatchIndex.builder()
                                             .start(matcher.start(group))
                                             .end(matcher.end(group))
                                             .build();
    }

    private String replace(final String line, final ConstantLineDetails.MatchIndex matchIndex, final String newValue) {
        return new StringBuilder(line).replace(matchIndex.getStart(), matchIndex.getEnd(), newValue).toString();
    }

    private String replace(final String line, final ConstantLineDetails.MatchIndex matchIndex1, final String newValue1, final ConstantLineDetails.MatchIndex matchIndex2,
                           final String newValue2) {
        return new StringBuilder(line)
                .replace(matchIndex2.getStart(), matchIndex2.getEnd(), newValue2)
                .replace(matchIndex1.getStart(), matchIndex1.getEnd(), newValue1)
                .toString();
    }

    private String trim(@Nullable final String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
