package fr.chuckame.marlinfw.configurator;

import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.Constant;
import fr.chuckame.marlinfw.configurator.constant.ProfilePropertiesChangeAdapter;
import fr.chuckame.marlinfw.configurator.profile.ProfileProperties;
import fr.chuckame.marlinfw.configurator.profile.ProfilePropertiesParser;
import fr.chuckame.marlinfw.configurator.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.convert.ConversionService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class MarlinConfigurator implements ApplicationRunner {
    public static void main(final String[] args) {
        SpringApplication.run(MarlinConfigurator.class, args);
    }

    private final ProfilePropertiesChangeAdapter changeAdapter;
    private final LineChangeManager lineChangeManager;
    private final ProfilePropertiesParser profilePropertiesParser;
    private final ConversionService conversionService;
    private final FileHelper fileHelper;

    // get profileProperties
    // get lines of file
    // apply profileProperties in memory
    // prompt the user all changes (E/E&C/D) and ERRORs
    // prompt WARN when missing constants that are defined into profile
    // is no error, apply changes into files if user agree (or if --yes arg)

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        final Path profilePath = getRequiredArgValue("profile", Path.class, args);
        final List<Path> filesPath = getRequiredArgValues("files", Path.class, args);

        final ProfileProperties profile = profilePropertiesParser.parseFromFile(profilePath).block();
        final var wantedConstants = changeAdapter.getWantedConstants(profile);

        Flux.fromIterable(filesPath)
            .flatMap(filePath -> fileHelper.lines(null)
                                           .index()
                                           .concatMap(line -> lineChangeManager.prepareChange(line.getT2(), line.getT1().intValue(), wantedConstants))
                                           .collectList()
                                           .zipWith(Mono.just(filePath)))
        
        ;
    }


    public Flux<LineChange> prepareChanges(final Flux<String> lines, final ProfileProperties profile) {
        final Map<String, Constant> wantedConstants = changeAdapter.getWantedConstants(profile);
        return lines.index().concatMap(line -> lineChangeManager.prepareChange(line.getT2(), line.getT1().intValue(), wantedConstants));
    }

    public Flux<String> applyChanges(final Collection<LineChange> changes) {
        return Flux.fromIterable(changes).flatMap(lineChangeManager::applyChange);
    }

    private <T> List<T> getRequiredArgValues(final String name, final Class<T> type, final ApplicationArguments args) {
        return Mono.justOrEmpty(args.getOptionValues(name))
                   .flatMapIterable(Function.identity())
                   .flatMap(v -> Mono.justOrEmpty(conversionService.convert(v, type))
                                     .switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("Invalid value for argument --%s", name)))))
                   .collectList()
                   .switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("Missing --%s argument", name))))
                   .block();
    }

    private <T> T getRequiredArgValue(final String name, final Class<T> type, final ApplicationArguments args) {
        return Flux.fromIterable(getRequiredArgValues(name, type, args))
                   .single()
                   .onErrorMap(IndexOutOfBoundsException.class, e -> new IllegalArgumentException(String.format("Only one --%s is allowed", name)))
                   .block();
    }
}
