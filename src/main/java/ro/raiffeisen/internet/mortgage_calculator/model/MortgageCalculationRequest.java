package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MortgageCalculationRequest {

    @Schema(description = "A product's unique identification code", maxLength = 128, example = "275000", required = true)
    @Size(max = 128)
    @JsonProperty
    private String productCode;

    @Schema(title = "Amount",
            description = "The price of building provided by client",
            example = "example: OrderedMap { \"currency\": \"EUR\", \"amount\": 123 }")
    @JsonProperty
    private Amount loanAmount;

    @Schema(description = "Area", required = true)
    @JsonProperty
    private Area area;

    @Schema(description = "Income and other instalments", required = true)
    @JsonProperty
    private Income income;

    @Schema(description = "Period of loan", example = "36")
    @JsonProperty
    private int tenor;

    @Schema(description = "Client age",
            example = "36", required = true)
    @JsonProperty
    private int age;

    @Schema(description = "The client already has another house",
            example = "36", required = true)
    @JsonProperty
    private boolean owner;

    @Schema(description = "Down payment", example = "36")
    @JsonProperty
    private BigDecimal downPayment;

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type",
            visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = VariableInterestRateType.class, name = "VARIABLE"),
            @JsonSubTypes.Type(value = MixedInterestRateType.class, name = "MIXED"),
    })
    @Schema(description = "The type of interest rate that is used for the loan. It can be either fix, variable but also a mixture of both")
    @JsonProperty
    private InterestRateType interestRateType;

    @Schema(description = "A property determining whether an insurance policy is included in the financing product",
            example = "true")
    @JsonProperty
    private boolean hasInsurance;

    @Schema(description = """
            The recalculation method of the repayment.

            EQUAL_PAYMENTS The repayments (installment + interest) are of equal size. They will change only if the interest rate changes.
            EQUAL_INSTALLEMTNS The installments are of equal size. The repayment amount varies in accordance with the interest.
            FIXED_ANNUITY_LOAN All repayments are of equal size throughout the loan period. The loan period varies in accordance with the reference rate.""")
    @JsonProperty
    InstallmentType installmentType;

    @Schema(description = "The set of properties required by NWB to determine special offer conditions.")
    @JsonProperty
    private SpecialOfferRequirements specialOfferRequirements;
}
