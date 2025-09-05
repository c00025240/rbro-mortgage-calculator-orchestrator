package ro.raiffeisen.internet.mortgage_calculator.model.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;

import java.util.List;

@Builder
@Data
public class InterestRateAdditionalInfo {
    /** Dobanda fixa */
    @JsonProperty
    private double interestRate;

    @JsonProperty private double bankMarginRate;
    @JsonProperty private double defaultBankMarginRate;

    /** Dobanda variabila */
    @JsonProperty private double variableInterestAfterFixedInterest;
    @JsonProperty private double defaultInterestRate;

    /** Dobanda variabila */
    @JsonProperty private double defaultVariableInterestAfterFixedInterest;
    @JsonProperty private Integer yearsWithFixedInterest;
    @JsonProperty private List<Discount> discounts;
}
