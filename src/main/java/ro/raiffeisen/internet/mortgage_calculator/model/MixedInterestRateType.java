package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
@Builder
public class MixedInterestRateType implements InterestRateType {

    @Schema(required = true)
    @JsonProperty
    private Type type;

    @Schema(description = "Id of the interest rate type in the NWB system", maxLength = 128)
    @JsonProperty
    private double interestRate;

    @Schema(description = "A fixed part defined in month of mixed interest rate type.", example = "12")
    @JsonProperty
    private int fixedPeriod;

    @AllArgsConstructor
    public enum Type {

        MIXED("MIXED");

        @Getter
        @JsonValue
        private final String value;

        @Override
        public String toString() {
            return String.valueOf(value);
        }

    }

}
