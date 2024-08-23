package de.malkusch.telgrambot;

import static java.util.Objects.requireNonNull;

public record Command(String name) {

    public Command {
        requireNonNull(name);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
    }

}
