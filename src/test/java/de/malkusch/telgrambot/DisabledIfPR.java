package de.malkusch.telgrambot;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "GITHUB_EVENT_NAME", matches = "pull_request")
@Retention(RetentionPolicy.RUNTIME)
public @interface DisabledIfPR {

}
