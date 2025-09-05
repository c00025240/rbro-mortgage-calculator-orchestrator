package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanCosts {

    @Schema(description = "An array of fees.")
    @JsonProperty
    private List<Fee> fees;

    @Schema(description = "A nominal value for the premium insurance policy payment.")
    @JsonProperty
    private List<LifeInsurance> lifeInsurance;

    @Schema
    @JsonProperty
    private DiscountsValues discounts;

    @Schema
    @JsonProperty
    private TotalDiscountsValues totalDiscountsValues;
}
