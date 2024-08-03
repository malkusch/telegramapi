package de.malkusch.telgrambot;

public record Callback(Command command, String data) {

    public Callback(Command command) {
        this(command, "null");
    }

    public static Callback parse(String callback) {
        var parsed = callback.split(":", 2);
        var command = new Command(parsed[0]);
        var data = parsed[1];
        return new Callback(command, data);
    }

    @Override
    public String toString() {
        return command.name() + ":" + data;
    }
}
