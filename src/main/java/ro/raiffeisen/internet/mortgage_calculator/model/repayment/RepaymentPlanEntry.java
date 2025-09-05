package ro.raiffeisen.internet.mortgage_calculator.model.repayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.raiffeisen.internet.mortgage_calculator.model.Amount;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentPlanEntry {

    /**
     *
     */
    @Schema(description = "An integer value of the repayment's month",
            minimum = "0",
            maximum = "600",
            required = true)
    @JsonProperty
    private int month;

    /**
     * principal
     * */
    @Schema(description = "An amount of the reimbursed capital for a month.",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount reimbursedCapitalAmount;

    /**
     * dobanda
     * */
    @Schema(description = "An amount of the interest for a month.",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount interestAmount;

    /**
     * comisioane
     * */
    @Schema(description = "An amount of fees for a month.",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount feeAmount;

    /**
     * rata lunara fara comision
     * */
    @Schema(description = "An amount of the monthly installment.",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount installmentAmount;

    /**
     * valoare totala de plata
     * */
    @Schema(description = "An amount of the total payment for a month.",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount totalPaymentAmount;

    /**
     * sold
     * */
    @Schema(description = "An amount of the loan left to be payed for this month.",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount remainingLoanAmount;
}
