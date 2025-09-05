package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TotalDiscountsValues {

    @JsonProperty
    private BigDecimal totalDiscountInstallment;
    @JsonProperty
    private BigDecimal totalDiscountAmount;
}
