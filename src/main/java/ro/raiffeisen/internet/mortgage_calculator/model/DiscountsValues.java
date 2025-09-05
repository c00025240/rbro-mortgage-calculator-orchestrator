package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class DiscountsValues {

    @JsonProperty
    private BigDecimal discountAmountHasSalaryInTheBank;
    @JsonProperty
    private BigDecimal discountAmountCasaVerde;
    @JsonProperty
    private BigDecimal discountAmountInsurance;
    @JsonProperty
    private BigDecimal discountAmountDownPayment;
}
