package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@Data
public class Amount {

    @Schema(description = "ISO 4217 Alpha 3 currency code.",
            maxLength = 256,
            pattern = "[A-Z]{3}",
            example = "EUR",
            required = true)
    @JsonProperty
    private String currency;

    @Schema(description = "The amount given with fractional digits, where fractions " +
                    "must be compliant to the currency definition. Up to 14 significant figures. " +
                    "Negative amounts are signed by minus. The decimal separator is a dot.",
            pattern = "-?[0-9]{1,14}(\\.[0-9]{1,3})?",
            example = "5877.78",
            required = true)
    private BigDecimal amount;

}
