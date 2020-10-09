package fr.chuckame.marlinfw.configurator;

import fr.chuckame.marlinfw.configurator.change.LineChange;
import fr.chuckame.marlinfw.configurator.change.LineChangeManager;
import fr.chuckame.marlinfw.configurator.constant.ProfilePropertiesChangeAdapter;
import fr.chuckame.marlinfw.configurator.profile.ProfileProperties;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class Configurator {
    // get lines of file
    // get profileProperties
    // apply profileProperties in memory
    // prompt the user all changes (E/E&C/D) and ERRORs
    // prompt WARN when missing constants that are defined into profile
    // is no error, apply changes into files if user agree (or if --yes arg)

    private final ProfilePropertiesChangeAdapter changeAdapter;
    private final LineChangeManager lineChangeManager;

    public Flux<LineChange> prepareChanges(final Flux<String> lines, final ProfileProperties profile) {
        final var wantedConstants = changeAdapter.getWantedConstants(profile);
        return lines.index().concatMap(line -> lineChangeManager.prepareChange(line.getT2(), line.getT1().intValue(), wantedConstants));
    }

    public Flux<String> applyChanges(final Flux<LineChange> changes) {
        return changes.map(lineChangeManager::applyChange);
    }
}
