package de.malkusch.telgrambot;

import static java.util.Objects.requireNonNull;

public record Callback(Command command, String data) {

    public Callback(Command command) {
        this(command, "null");
    }

    public Callback {
        requireNonNull(command);
        requireNonNull(data);
        if (data.isBlank()) {
            throw new IllegalArgumentException("data must not be empty");
        }
    }

    public static Callback parse(String callback) {
        requireNonNull(callback);
        if (callback.isBlank()) {
            throw new IllegalArgumentException("callback must not be empty");
        }

        var parsed = callback.split(":", 2);
        if (parsed.length != 2) {
            throw new IllegalArgumentException(String.format("callback '%s' could not be parsed", callback));
        }

        var command = new Command(parsed[0]);
        var data = parsed[1];
        return new Callback(command, data);
    }

    @Override
    public String toString() {
        return command.name() + ":" + data;
    }
}
