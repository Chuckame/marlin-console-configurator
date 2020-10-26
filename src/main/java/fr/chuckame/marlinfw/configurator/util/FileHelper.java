package fr.chuckame.marlinfw.configurator.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileHelper {
    public Flux<String> lines(final Path file) {
        return Flux.fromStream(ExceptionUtils.wrap(() -> Files.lines(file)));
    }

    public Mono<byte[]> read(final Path file) {
        return Mono.fromCallable(() -> Files.readAllBytes(file));
    }

    public Mono<Void> write(final byte[] bytes, final Path file) {
        return Mono.fromSupplier(ExceptionUtils.wrap(() -> Files.write(file, bytes, StandardOpenOption.CREATE_NEW))).then();
    }
}
