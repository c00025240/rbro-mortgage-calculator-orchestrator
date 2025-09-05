package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Data
@Builder
public class MortgageCalculationResponse {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type",
            visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VariableInterestRateType.class, name = "VARIABLE"),
            @JsonSubTypes.Type(value = MixedInterestRateType.class, name = "MIXED"),
    })
    @Schema(
            description = "The type of interest rate that is used for the loan. It can be either fix, variable but also a mixture of both")
    @JsonProperty
    private InterestRateType interestRateType;

    /**
     * rata dobanzii
     * */
    @Schema(description = "A percentage rate of nominal interest that a customer will pay over the loan life-time.",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private BigDecimal nominalInterestRate;

    /**
     * rata dobanzii
     * */
    @Schema(description = "A percentage rate of nominal interest that a customer will pay over the loan life-time.",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private InterestRateFormula interestRateFormula;

    /**
     * Valoarea solicitata fara comision
     * */
    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount loanAmount;

    /**
     * Valoarea totala pe care o poate impumuta in functie de venit,gradul de indatorare si dobanda
     * */
    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount maxAmount;

    /**
     * Avans (by default 30%)
     * */
    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount downPayment;

    /**
     * Valoarea solicitata plus comision de analiza
     * */
    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount loanAmountWithFee;

    /**
     * Valoarea solicitata plus avans
     * */
    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount housePrice;

    /**
     * Suma lunara de plata
     * */
    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount totalPaymentAmount;

    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private int tenor;

    @Schema(example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private MonthlyInstallment monthlyInstallment;

    /**
     * PremiumInsurance.value = Valoare asigurare
     * */
    @Schema(description = "All loan's costs.")
    @JsonProperty
    private LoanCosts loanCosts;

    /**
     * DAE
     * */
    @Schema(description = "A percentage rate of the principal, so it represents the actual yearly cost of funds over the term of a loan",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private BigDecimal annualPercentageRate;

    @Schema(description = "A percentage rate of the principal, so it represents the actual yearly cost of funds over the term of a loan",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private BigDecimal noDocAmount;

    @Schema(description = "A percentage rate of the principal, so it represents the actual yearly cost of funds over the term of a loan",
            minimum = "0",
            maximum = "100")
    @JsonProperty
    private BigDecimal minGuaranteeAmount;

    @Schema(description = "The set of properties related to commission descriptions.")
    @JsonProperty
    private CommissionDescription commissionDescription;
}
