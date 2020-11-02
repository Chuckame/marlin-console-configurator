package fr.chuckame.marlinfw.configurator.util;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

@Component
public class FileHelper {
    public Flux<Path> listFiles(final List<Path> paths) {
        return Flux.fromIterable(paths)
                   .flatMap(path -> {
                       if (Files.isDirectory(path)) {
                           return toFlux(ExceptionUtils.wrap(() -> Files.newDirectoryStream(path).spliterator()));
                       }
                       return Mono.just(path);
                   })
                   .filter(Files::isRegularFile)
                ;
    }

    private <T> Flux<T> toFlux(final Supplier<Spliterator<T>> iterator) {
        return Flux.fromStream(() -> StreamSupport.stream(iterator.get(), false));

    }

    public Flux<String> lines(final Path file) {
        return Flux.fromStream(ExceptionUtils.wrap(() -> Files.lines(file)));
    }

    public Mono<byte[]> read(final Path file) {
        return Mono.fromCallable(() -> Files.readAllBytes(file));
    }

    public Mono<Void> write(final byte[] bytes, final Path file) {
        return Mono.fromSupplier(ExceptionUtils.wrap(() -> Files.write(file, bytes, StandardOpenOption.CREATE_NEW))).then();
    }

    public Mono<Void> write(final Path file, final boolean override, final Flux<String> lines) {
        return lines.collectList()
                    .defaultIfEmpty(List.of())
                    .flatMap(l -> (override && Files.exists(file) ? detectLineSeparator(file) : Mono.<String>empty()).defaultIfEmpty(System.lineSeparator())
                                                                                                                     .map(lineSeparator -> String.join(lineSeparator, l)
                                                                                                                                                 .concat(lineSeparator)))
                    .flatMap(linesList ->
                                     Mono.fromSupplier(ExceptionUtils.wrap(() ->
                                                                                   Files.write(file, linesList
                                                                                           .getBytes(StandardCharsets.UTF_8), override ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE_NEW))))
                    .then();
    }

    public Mono<String> detectLineSeparator(final Path file) {
        return Mono.fromCallable(() -> retrieveLineSeparator(file.toFile()));
    }

    private static String retrieveLineSeparator(final File file) throws IOException {
        char current;
        final StringBuilder lineSeparator = new StringBuilder();
        try (final FileInputStream fis = new FileInputStream(file)) {
            while (fis.available() > 0) {
                current = (char) fis.read();
                if ((current == '\n') || (current == '\r')) {
                    lineSeparator.append(current);
                    if (fis.available() > 0) {
                        final char next = (char) fis.read();
                        if ((next != current)
                                && ((next == '\r') || (next == '\n'))) {
                            lineSeparator.append(next);
                        }
                    }
                    return lineSeparator.toString();
                }
            }
        }
        return null;
    }
}
