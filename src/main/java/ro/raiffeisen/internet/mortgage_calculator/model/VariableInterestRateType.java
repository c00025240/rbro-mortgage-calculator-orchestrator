package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@AllArgsConstructor
public class VariableInterestRateType implements InterestRateType {

    @Schema(required = true)
    @JsonProperty
    private Type type;

    @Schema(description = "The interest rate that applies after the fixed period of the loan period",
            maximum = "100",
            minimum = "0")
    @JsonProperty
    private double interestRate;

    @AllArgsConstructor
    public enum Type {

        VARIABLE("VARIABLE");

        @Getter
        @JsonValue
        private final String value;

        @Override
        public String toString() {
            return String.valueOf(value);
        }

    }
}
