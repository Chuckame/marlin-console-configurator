package fr.chuckame.marlinfw.configurator.util;

import reactor.core.Exceptions;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExceptionUtils {
    public static <I, O> Function<I, O> wrap(final CheckedFunction<I, O> function) {
        return in -> {
            try {
                return function.apply(in);
            } catch (final Exception e) {
                throw Exceptions.propagate(e);
            }
        };
    }

    public static <T> Supplier<T> wrap(final Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (final Exception e) {
                throw Exceptions.propagate(e);
            }
        };
    }

    public interface CheckedFunction<I, O> {
        O apply(I in) throws Exception;
    }
}
