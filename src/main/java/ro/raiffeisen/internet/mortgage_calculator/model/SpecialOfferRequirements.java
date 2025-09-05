package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecialOfferRequirements {

    @Schema(description = " A property determining whether a loan calculation user have salary in the bank, to provide a special loan offer",
            example = "false")
    @JsonProperty
    private boolean hasSalaryInTheBank;

    @Schema(description = " A property determining whether a loan calculation user want a green house")
    @JsonProperty
    private boolean casaVerde;

}
