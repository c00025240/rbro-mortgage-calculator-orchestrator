package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CommissionDescription {

    @Schema(description = "A text field to describe assessment fee.",
            maxLength = 256,
            example = "90 Euro + VAT (equivalent in Lei at the BNR exchange rate from the date of payment)")
    @Size(max = 256)
    @JsonProperty
    private String assessmentFee;

    @Schema(description = "A text field to describe the single commission after granting.",
            maxLength = 256,
            example = "90 Euros (equivalent in Lei at the BNR exchange rate from the date of payment)")
    @Size(max = 256)
    @JsonProperty
    private String grantingFee;

    @Schema(description = "A text field to describe the commission to be payed for a dedication funding support.",
            maxLength = 256,
            example = "0.30% of the value of the guarantee granted by the Romanian state (50% of credit balance)")
    @Size(max = 256)
    @JsonProperty
    private String guaranteePromiseCommission;

    @Schema(description = "A text field to describe the criteria for a one time payment to be provided to the bank if the borrower would repay earlier then agreed.",
            maxLength = 256)
    @Size(max = 256)
    @JsonProperty
    private String earlyRepaymentCommission;

    @Schema(description = "The insurance cost calculated formula. Might be dependent on Accept-Language Header.",
            maxLength = 256)
    @Size(max = 256)
    @JsonProperty
    private String insuranceCostCalculationFormula;

    @Schema(description = "Text field to display an interest rate calculation in detail. Might be dependent on Accept-Language Header.",
            maxLength = 1024)
    @Size(max = 1024)
    @JsonProperty
    private String interestRateDescription;
}
