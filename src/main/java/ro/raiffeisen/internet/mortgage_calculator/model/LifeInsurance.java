package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LifeInsurance {

    @Schema
    @JsonProperty
    private Amount value;

    @Schema
    @JsonProperty
    private Frequency paymentFrequency;

}
