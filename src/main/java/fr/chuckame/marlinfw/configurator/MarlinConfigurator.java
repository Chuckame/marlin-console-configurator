package fr.chuckame.marlinfw.configurator;

import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeFormatter;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ProfilePropertiesChangeAdapter;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SpringBootApplication
@RequiredArgsConstructor
public class MarlinConfigurator implements ApplicationRunner {
    public static void main(final String[] args) {
        SpringApplication.run(MarlinConfigurator.class, args);
    }

    private final ProfilePropertiesChangeAdapter changeAdapter;
    private final LineChangeManager lineChangeManager;
    private final LineChangeFormatter lineChangeFormatter;
    private final ProfilePropertiesParser profilePropertiesParser;
    private final FileHelper fileHelper;

    // get profileProperties
    // get lines of file
    // prepare profileProperties in memory
    // prompt the user all changes (E/E&C/D) and ERRORs
    // prompt WARN when missing constants that are defined into profile
    // is no error, apply changes into files if user agree (or if --yes arg)

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        try {
            final Path profilePath = getRequiredArgPath("profile", args);
            final List<Path> filesPath = getRequiredArgPaths("file", args);
            final boolean forceApply = args.containsOption("apply");

            profilePropertiesParser.parseFromFile(profilePath)
                                   .map(changeAdapter::getWantedConstants)
                                   .flatMap(wantedConstants ->
                                                    prepareChanges(filesPath, wantedConstants)
                                                            .flatMap(changes -> printChanges(changes)
                                                                    .then(printUnusedConstants(changes, wantedConstants))
                                                                    .then(checkIfUserAgree(forceApply))
                                                                    .then(applyAndSaveChanges(changes)))
                                   )
                                   .blockOptional()
            ;
        } catch (final InvalidArgException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private Mono<Void> printChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .concatMap(fileChanges -> Flux.concat(
                           Flux.just(String.format("%s change(s) for file %s:", fileChanges.getValue().size(), fileChanges.getKey())),
                           Flux.fromIterable(fileChanges.getValue()).filter(LineChange::isConstant).map(lineChangeFormatter::format),
                           Flux.just("")
                   ))
                   .doOnNext(System.out::println)
                   .then()
                ;
    }

    private Mono<Void> printUnusedConstants(final Map<Path, List<LineChange>> changes, final Map<String, Constant> wantedConstants) {
        return lineChangeManager.getUnusedWantedConstants(changes.values().stream().flatMap(List::stream).collect(Collectors.toList()), wantedConstants)
                                .collectList()
                                .filter(Predicate.not(List::isEmpty))
                                .doOnNext(unusedConstants -> System.out.printf("Still some unused constants: %s%n", unusedConstants))
                                .then()
                ;
    }

    private Mono<Void> checkIfUserAgree(final boolean forceApply) {
        if (forceApply) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> System.out.println("Apply changes ? type 'y' to apply changes, or everything else to cancel"))
                   .then(Mono.fromSupplier(() -> new Scanner(System.in).next()))
                   .filter("y"::equals)
                   .switchIfEmpty(Mono.error(() -> new InvalidArgException("User refused to apply")))
                   .then();
    }

    public Mono<Map<Path, List<LineChange>>> prepareChanges(final List<Path> filesPath, final Map<String, Constant> wantedConstants) {
        return Flux.fromIterable(filesPath)
                   .flatMap(filePath -> fileHelper.lines(filePath)
                                                  .index()
                                                  .concatMap(line -> lineChangeManager.prepareChange(line.getT2(), line.getT1().intValue(), wantedConstants))
                                                  .collectList()
                                                  .zipWith(Mono.just(filePath)))
                   .collectMap(Tuple2::getT2, Tuple2::getT1);
    }

    public Mono<Void> applyAndSaveChanges(final Map<Path, List<LineChange>> changes) {
        return Flux.fromIterable(changes.entrySet())
                   .groupBy(Map.Entry::getKey, Map.Entry::getValue)
                   .flatMap(fileChanges -> fileHelper.write(fileChanges.key(), true, fileChanges.flatMap(this::applyChanges)))
                   .then();
    }

    public Flux<String> applyChanges(final Collection<LineChange> changes) {
        return Flux.fromIterable(changes).flatMap(lineChangeManager::applyChange);
    }

    private List<Path> getRequiredArgPaths(final String name, final ApplicationArguments args) {
        return Mono.justOrEmpty(args.getOptionValues(name))
                   .flatMapIterable(Function.identity())
                   .flatMap(v -> Mono.fromSupplier(() -> Path.of(v))
                                     .doOnError(e -> new InvalidArgException("Invalid value for argument --%s: %s", name, e.getMessage())))
                   .collectList()
                   .filter(Predicate.not(List::isEmpty))
                   .switchIfEmpty(Mono.error(() -> new InvalidArgException("Missing --%s argument", name)))
                   .block();
    }

    private Path getRequiredArgPath(final String name, final ApplicationArguments args) {
        return Flux.fromIterable(getRequiredArgPaths(name, args))
                   .single()
                   .onErrorMap(IndexOutOfBoundsException.class, e -> new InvalidArgException("Only one --%s is allowed", name))
                   .block();
    }
}

class InvalidArgException extends RuntimeException {
    public InvalidArgException(final String format, final Object... args) {
        super(String.format(format, args));
    }
}
