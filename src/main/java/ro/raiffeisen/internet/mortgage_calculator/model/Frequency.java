package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Frequency {

    ONE_TIME("ONE_TIME"),

    ONE_TIME_RESPONSE("ONE_TIME"),
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    SEMI_ANNUALLY("SEMI_ANNUALLY"),
    ANNUALLY("ANNUALLY");

    @Getter
    @JsonValue
    private final String value;

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static Frequency fromValue(String input) {
        for (Frequency b : Frequency.values()) {
            if (b.value.equals(input)) {
                return b;
            }
        }
        return null;
    }
}
