package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Fee {

    @Schema(description = "A type of a fee")
    @JsonProperty
    FeeType type;

    @Schema(title = "Amount",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }",
            required = true)
    @JsonProperty
    private Amount fixedAmount;

    @Schema(description = "A fee frequency")
    @JsonProperty
    private Frequency frequency;

    @AllArgsConstructor
    public enum FeeType {
        LOAN_APPROVAL,
        SUCCESSIVE_USAGE,
        UTILIZATION_PROLONGATION,
        PREMATURE_REPAYMENT,
        OTHER_CHANGES,
        CANCELLATION,
        UNUSED_LOAN_AMOUNT,
        REMINDER,
        COMMISSION,
        ADMINISTRATION,
        ACCOUNT,
        MANAGEMENT,
        REPAYMENT_PROLONGATION,
        PENALTY;
    }
}
