package fr.chuckame.marlinfw.configurator.command;

import reactor.core.publisher.Mono;

public interface Command {
    Mono<Void> run();
}
