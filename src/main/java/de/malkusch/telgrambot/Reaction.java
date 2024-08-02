package de.malkusch.telgrambot;

public record Reaction(String emoji) {
    public static final Reaction THUMBS_UP = new Reaction("👍");
    public static final Reaction UNKNOWN = new Reaction(null);
}
