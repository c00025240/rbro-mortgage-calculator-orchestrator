package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
public class MonthlyInstallment {

    @Schema(description = "A percentage rate of nominal interest that a customer will pay over the loan life-time.",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private BigDecimal amountFixedInterest;

    @Schema(description = "A percentage rate of nominal interest that a customer will pay over the loan life-time.",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private BigDecimal amountVariableInterest;
}
