package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
@Data
@Builder
@AllArgsConstructor
public class InterestRateFormula {

    @Schema(description = "A percentage rate of nominal interest that a customer will pay over the loan life-time.",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private double bankMarginRate;

    @Schema(description = "A percentage rate of nominal interest that a customer will pay over the loan life-time.",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private double irccRate;

}
