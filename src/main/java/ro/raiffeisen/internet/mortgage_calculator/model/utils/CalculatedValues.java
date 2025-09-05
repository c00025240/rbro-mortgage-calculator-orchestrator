package ro.raiffeisen.internet.mortgage_calculator.model.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
public class CalculatedValues {

    @JsonProperty
    private BigDecimal variableDiscountAmountHasSalaryInTheBank;
    @JsonProperty
    private BigDecimal variableDiscountAmountCasaVerde;
    @JsonProperty
    private BigDecimal variableDiscountAmountInsurance;
    @JsonProperty
    private BigDecimal variableDiscountAmountDownPayment;
}
