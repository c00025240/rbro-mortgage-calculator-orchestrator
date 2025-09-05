package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum InstallmentType {

    DECREASING_INSTALLMENTS("DECREASING_INSTALLMENTS"),
    EQUAL_INSTALLMENTS("EQUAL_INSTALLMENTS");

    @Getter
    @JsonValue
    private final String value;

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static InstallmentType fromValue(String input) {
        for (InstallmentType b : InstallmentType.values()) {
            if (b.value.equals(input)) {
                return b;
            }
        }
        return null;
    }
}
