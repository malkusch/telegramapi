package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.api.InternalTelegramApi.Decorator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalTelegramApiTest {

    private final Decorator<Integer> addOne = it -> it + 1;

    @Test
    void decorator_DecorateShouldDecorate() {
        var result = addOne.decorate(1);
        assertEquals(2, result);
    }

    @Test
    void decorator_thenShouldDecorate() {
        var multiply = addOne.then(it -> it * 10);

        var result = multiply.decorate(1);
        assertEquals((1 + 1) * 10, result);
    }
}
