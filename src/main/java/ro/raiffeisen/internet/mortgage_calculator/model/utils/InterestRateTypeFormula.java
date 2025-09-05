package ro.raiffeisen.internet.mortgage_calculator.model.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class InterestRateTypeFormula {
    @JsonProperty
    private double interestRate;

    @JsonProperty
    private double bankMarginRate;

    @JsonProperty
    private double variableInterestAfterFixedInterest;

}
