package de.malkusch.telgrambot;

public record Reaction(String emoji) {
    public static final Reaction THUMBS_UP = new Reaction("\uD83D\uDC4D");
    public static final Reaction UNKNOWN = new Reaction(null);
}
