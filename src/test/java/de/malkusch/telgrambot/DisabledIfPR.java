package de.malkusch.telgrambot;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "GITHUB_TRIGGERING_ACTOR", matches = "malkusch")
@Retention(RetentionPolicy.RUNTIME)
public @interface DisabledIfPR {

}
